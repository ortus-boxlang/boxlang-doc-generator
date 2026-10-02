package ortus.boxlang.tools.util;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ParamTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;

/**
 * Reads the javadocs for JDK classes straight from the JDK source ( java.base only ).
 * <p>
 * The sources come from the local <code>src.zip</code> when the running JDK matches the requested version, otherwise the single
 * source file is downloaded from the matching OpenJDK tag and cached in the temp directory.
 */
public class JavaClassDocs {

	private static final String						SOURCE_URL			= "https://raw.githubusercontent.com/openjdk/jdk/jdk-%d-ga/src/java.base/share/classes/%s.java";
	private static final int						MAX_INHERIT_DEPTH	= 4;
	private static final Pattern					RETURN_TAG			= Pattern.compile( "\\{@return\\s+([^}]*)\\}" );
	private static final Pattern					INHERIT_TAG			= Pattern.compile( "\\{@inheritDoc\\s*\\}" );
	private static final Pattern					PLAIN_TAG			= Pattern.compile( "\\{@(?:index|value|systemProperty|summary)\\s*([^}]*)\\}" );
	private static final Pattern					SPEC_TAG			= Pattern.compile( "\\{@(jls|jvms)\\s+([^}]*)\\}" );
	// Javadoc links relative to the JDK documentation, which do not resolve in our docs
	private static final Pattern					RELATIVE_LINK		= Pattern.compile( "<a\\s+href=\"(?!https?:)[^\"]*\"\\s*>(.*?)</a>", Pattern.DOTALL );
	private static final Map<String, ParsedType>	parsedTypes			= new HashMap<>();

	/**
	 * A documented public method
	 *
	 * @param name        The method name
	 * @param signature   The method signature, without modifiers other than static
	 * @param description The javadoc description, which may contain inline javadoc tags
	 * @param parameters  The documented parameters by name
	 * @param returns     The documented return value, or an empty string
	 */
	public record MethodDoc( String name, String signature, String description, Map<String, String> parameters, String returns ) {
	}

	private record ParsedType( CompilationUnitTree unit, ClassTree tree, DocTrees trees ) {
	}

	/**
	 * The JDK version to document, from the <code>jdkVersion</code> in <code>gradle.properties</code> of the working directory.
	 * Falls back to the version of the running JDK.
	 */
	public static int getJdkVersion() {
		Path properties = Path.of( "gradle.properties" );
		if ( Files.exists( properties ) ) {
			try ( var reader = Files.newBufferedReader( properties ) ) {
				Properties props = new Properties();
				props.load( reader );
				String version = props.getProperty( "jdkVersion" );
				if ( version != null ) {
					return Integer.parseInt( version.trim() );
				}
			} catch ( IOException | NumberFormatException e ) {
				System.err.println( "Unable to read jdkVersion from gradle.properties: " + e.getMessage() );
			}
		}
		return Runtime.version().feature();
	}

	/**
	 * The first sentence of the class javadoc, or an empty string if unavailable
	 */
	public static String getClassSummary( String className, int jdkVersion ) {
		ParsedType type = parse( className, jdkVersion );
		if ( type == null ) {
			return "";
		}
		DocCommentTree comment = type.trees().getDocCommentTree( classPath( type ) );
		return comment == null ? "" : text( comment.getFirstSentence() );
	}

	/**
	 * The public, non-deprecated, documented methods of a JDK class. Methods without their own javadoc inherit it from the
	 * supertypes, as the javadoc tool does.
	 */
	public static List<MethodDoc> getPublicMethods( String className, int jdkVersion ) {
		ParsedType type = parse( className, jdkVersion );
		if ( type == null ) {
			return List.of();
		}

		boolean			isInterface	= type.tree().getKind() == Tree.Kind.INTERFACE;
		List<MethodDoc>	methods		= new ArrayList<>();
		for ( Tree member : type.tree().getMembers() ) {
			if ( ! ( member instanceof MethodTree method ) || method.getName().contentEquals( "<init>" ) ) {
				continue;
			}
			var		flags		= method.getModifiers().getFlags();
			boolean	isPublic	= flags.contains( Modifier.PUBLIC ) || ( isInterface && !flags.contains( Modifier.PRIVATE ) );
			if ( !isPublic ) {
				continue;
			}

			DocCommentTree comment = findComment( type, method, jdkVersion, 0 );
			if ( comment == null || isDeprecated( method, comment ) ) {
				continue;
			}

			Map<String, String>	parameters	= new LinkedHashMap<>();
			String				returns		= "";
			for ( DocTree tag : comment.getBlockTags() ) {
				if ( tag instanceof ParamTree param && !param.isTypeParameter() ) {
					parameters.put( param.getName().toString(), text( param.getDescription() ) );
				} else if ( tag instanceof ReturnTree returnTag ) {
					returns = text( returnTag.getDescription() );
				}
			}

			methods.add( new MethodDoc( method.getName().toString(), signature( method ), text( comment.getFullBody() ), parameters, returns ) );
		}

		methods.sort( Comparator.comparing( MethodDoc::name, String.CASE_INSENSITIVE_ORDER ).thenComparing( MethodDoc::signature ) );
		return methods;
	}

	private static TreePath classPath( ParsedType type ) {
		return new TreePath( new TreePath( type.unit() ), type.tree() );
	}

	private static DocCommentTree findComment( ParsedType type, MethodTree method, int jdkVersion, int depth ) {
		DocCommentTree comment = type.trees().getDocCommentTree( new TreePath( classPath( type ), method ) );
		if ( comment != null || depth >= MAX_INHERIT_DEPTH ) {
			return comment;
		}

		for ( String superName : getSuperTypeNames( type ) ) {
			ParsedType superType = parse( superName, jdkVersion );
			if ( superType == null ) {
				continue;
			}
			MethodTree inherited = findMethod( superType, method );
			if ( inherited != null ) {
				DocCommentTree superComment = findComment( superType, inherited, jdkVersion, depth + 1 );
				if ( superComment != null ) {
					return superComment;
				}
			}
		}
		return null;
	}

	private static MethodTree findMethod( ParsedType type, MethodTree target ) {
		List<String>	targetTypes	= parameterTypes( target );
		MethodTree		sameArity	= null;
		for ( Tree member : type.tree().getMembers() ) {
			if ( member instanceof MethodTree candidate && candidate.getName().contentEquals( target.getName() )
			    && candidate.getParameters().size() == target.getParameters().size() ) {
				if ( parameterTypes( candidate ).equals( targetTypes ) ) {
					return candidate;
				}
				sameArity = candidate;
			}
		}
		return sameArity;
	}

	private static List<String> parameterTypes( MethodTree method ) {
		return method.getParameters().stream().map( parameter -> parameter.getType().toString() ).toList();
	}

	private static List<String> getSuperTypeNames( ParsedType type ) {
		List<Tree> clauses = new ArrayList<>();
		if ( type.tree().getExtendsClause() != null ) {
			clauses.add( type.tree().getExtendsClause() );
		}
		clauses.addAll( type.tree().getImplementsClause() );

		String			packageName	= type.unit().getPackageName() == null ? "" : type.unit().getPackageName().toString();
		List<String>	names		= new ArrayList<>();
		for ( Tree clause : clauses ) {
			String simpleName = clause.toString().replaceAll( "<.*", "" ).trim();
			if ( simpleName.contains( "." ) ) {
				names.add( simpleName );
				continue;
			}
			String qualified = packageName + "." + simpleName;
			for ( ImportTree importTree : type.unit().getImports() ) {
				String imported = importTree.getQualifiedIdentifier().toString();
				if ( imported.endsWith( "." + simpleName ) ) {
					qualified = imported;
					break;
				}
			}
			names.add( qualified );
		}
		return names;
	}

	private static boolean isDeprecated( MethodTree method, DocCommentTree comment ) {
		return method.getModifiers().getAnnotations().stream().anyMatch( annotation -> annotation.getAnnotationType().toString().endsWith( "Deprecated" ) )
		    || comment.getBlockTags().stream().anyMatch( tag -> tag.getKind() == DocTree.Kind.DEPRECATED );
	}

	private static String signature( MethodTree method ) {
		StringBuilder signature = new StringBuilder();
		if ( method.getModifiers().getFlags().contains( Modifier.STATIC ) ) {
			signature.append( "static " );
		}
		if ( !method.getTypeParameters().isEmpty() ) {
			signature.append( "<" ).append( method.getTypeParameters().stream().map( Object::toString ).collect( Collectors.joining( ", " ) ) ).append( "> " );
		}
		signature.append( method.getReturnType() ).append( ' ' ).append( method.getName() ).append( '(' )
		    .append( method.getParameters().stream().map( parameter -> parameter.getType() + " " + parameter.getName() ).collect( Collectors.joining( ", " ) ) )
		    .append( ')' );
		return signature.toString();
	}

	/**
	 * Joins doc tree elements, resolving the inline tags which carry no markup of their own
	 */
	private static String text( List<? extends DocTree> trees ) {
		String joined = trees.stream().map( Object::toString ).collect( Collectors.joining() ).trim();
		joined	= RETURN_TAG.matcher( joined ).replaceAll( result -> "Returns " + Matcher.quoteReplacement( result.group( 1 ).trim() ) );
		joined	= INHERIT_TAG.matcher( joined ).replaceAll( "" );
		joined	= PLAIN_TAG.matcher( joined ).replaceAll( result -> Matcher.quoteReplacement( result.group( 1 ).trim() ) );
		joined	= SPEC_TAG.matcher( joined )
		    .replaceAll( result -> result.group( 1 ).toUpperCase() + " " + Matcher.quoteReplacement( result.group( 2 ).trim() ) );
		joined	= RELATIVE_LINK.matcher( joined ).replaceAll( result -> Matcher.quoteReplacement( result.group( 1 ) ) );
		return joined.trim();
	}

	private static ParsedType parse( String className, int jdkVersion ) {
		if ( parsedTypes.containsKey( className ) ) {
			return parsedTypes.get( className );
		}

		ParsedType	result	= null;
		String		source	= className.startsWith( "java." ) ? readSource( className, jdkVersion ) : null;
		if ( source != null ) {
			String			simpleName	= className.substring( className.lastIndexOf( '.' ) + 1 );
			JavaCompiler	compiler	= ToolProvider.getSystemJavaCompiler();
			JavaFileObject	file		= new SimpleJavaFileObject( URI.create( "string:///" + className.replace( '.', '/' ) + ".java" ),
			    JavaFileObject.Kind.SOURCE ) {

											@Override
											public CharSequence getCharContent( boolean ignoreEncodingErrors ) {
												return source;
											}
										};
			try {
				JavacTask task = ( JavacTask ) compiler.getTask( null, null, null, List.of( "-proc:none" ), null, List.of( file ) );
				for ( CompilationUnitTree unit : task.parse() ) {
					for ( Tree declaration : unit.getTypeDecls() ) {
						if ( declaration instanceof ClassTree classTree && classTree.getSimpleName().contentEquals( simpleName ) ) {
							result = new ParsedType( unit, classTree, DocTrees.instance( task ) );
						}
					}
				}
			} catch ( IOException e ) {
				System.err.println( "Unable to parse JDK source for " + className + ": " + e.getMessage() );
			}
		}

		parsedTypes.put( className, result );
		return result;
	}

	private static String readSource( String className, int jdkVersion ) {
		String	path	= className.replace( '.', '/' );
		Path	srcZip	= Path.of( System.getProperty( "java.home" ), "lib", "src.zip" );

		// The local sources are only usable when they are for the requested JDK version
		if ( Runtime.version().feature() == jdkVersion && Files.exists( srcZip ) ) {
			try ( ZipFile zip = new ZipFile( srcZip.toFile() ) ) {
				ZipEntry entry = zip.getEntry( "java.base/" + path + ".java" );
				if ( entry != null ) {
					return new String( zip.getInputStream( entry ).readAllBytes(), StandardCharsets.UTF_8 );
				}
			} catch ( IOException e ) {
				System.err.println( "Unable to read " + srcZip + ": " + e.getMessage() );
			}
		}

		Path cached = Path.of( System.getProperty( "java.io.tmpdir" ), "boxlang-doc-generator", "jdk-" + jdkVersion, path + ".java" );
		try {
			if ( !Files.exists( cached ) ) {
				HttpResponse<String> response = HttpClient.newHttpClient().send(
				    HttpRequest.newBuilder( URI.create( SOURCE_URL.formatted( jdkVersion, path ) ) ).build(),
				    HttpResponse.BodyHandlers.ofString( StandardCharsets.UTF_8 )
				);
				if ( response.statusCode() != 200 ) {
					System.err.println( "JDK " + jdkVersion + " source not found for " + className + " (HTTP " + response.statusCode() + ")" );
					return null;
				}
				Files.createDirectories( cached.getParent() );
				Files.writeString( cached, response.body(), StandardCharsets.UTF_8 );
			}
			return Files.readString( cached, StandardCharsets.UTF_8 );
		} catch ( IOException e ) {
			System.err.println( "Unable to retrieve JDK " + jdkVersion + " source for " + className + ": " + e.getMessage() );
		} catch ( InterruptedException e ) {
			Thread.currentThread().interrupt();
		}
		return null;
	}

}
