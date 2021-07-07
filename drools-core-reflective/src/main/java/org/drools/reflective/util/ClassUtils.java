/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.reflective.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.kie.api.definition.type.Modifies;
import org.kie.internal.utils.ClassLoaderUtil;

public abstract class ClassUtils {
    private static final ProtectionDomain  PROTECTION_DOMAIN;

    public static final boolean IS_ANDROID;

    private static final Map<String, Class<?>> classes = Collections.synchronizedMap( new HashMap() );

    private static final Map<String, Constructor<?>> constructors = Collections.synchronizedMap( new HashMap() );

    private static final String STAR    = "*";

    private static final Map<String, String> abbreviationMap;

    private static final Map<String, Class<?>> primitiveNameToType;

    protected ClassUtils() { }

    static {
        final Map<String, String> m = new HashMap<>();
        m.put("int", "I");
        m.put("boolean", "Z");
        m.put("float", "F");
        m.put("long", "J");
        m.put("short", "S");
        m.put("byte", "B");
        m.put("double", "D");
        m.put("char", "C");
        m.put("void", "V");
        final Map<String, String> r = new HashMap<>();
        for (final Map.Entry<String, String> e : m.entrySet()) {
            r.put(e.getValue(), e.getKey());
        }
        abbreviationMap = Collections.unmodifiableMap(m);

        final Map<String, Class<?>> m2 = new HashMap<>();
        m2.put("int", int.class);
        m2.put("boolean", boolean.class);
        m2.put("float", float.class);
        m2.put("long", long.class);
        m2.put("short", short.class);
        m2.put("byte", byte.class);
        m2.put("double", double.class);
        m2.put("char", char.class);
        primitiveNameToType = Collections.unmodifiableMap(m2);
    }

    static {
        PROTECTION_DOMAIN = (ProtectionDomain) AccessController.doPrivileged((PrivilegedAction) ClassLoaderUtil.class::getProtectionDomain);

        // determine if we are running on Android
        boolean isAndroid;
        try {
            isAndroid = loadClass("org.drools.android.DroolsAndroidContext", null) != null &&
                    loadClass("android.os.Build", null) != null &&
                    loadClass("dalvik.system.DexPathList", null) != null;
        } catch (Exception e) {
            isAndroid = false;
        }
        IS_ANDROID = isAndroid;
    }
    
    public static boolean areNullSafeEquals(Object obj1, Object obj2) {
        return obj1 == null ? obj2 == null : obj1.equals(obj2);
    }

    /**
     * Please do not use - internal
     * org/my/Class.xxx -> org.my.Class
     */
    public static String convertResourceToClassName(final String pResourceName) {
        return stripExtension(pResourceName).replace('/', '.');
    }

    /**
     * Please do not use - internal
     * org.my.Class -> org/my/Class.class
     */
    public static String convertClassToResourcePath(final Class cls) {
        return convertClassToResourcePath(cls.getName());
    }

    public static String convertClassToResourcePath(final String pName) {
        return pName.replace( '.',
                              '/' ) + ".class";
    }

    /**
     * Please do not use - internal
     * org/my/Class.xxx -> org/my/Class
     */
    public static String stripExtension(final String pResourceName) {
        final int i = pResourceName.lastIndexOf('.');
        return pResourceName.substring( 0, i );
    }

    public static String toJavaCasing(final String pName) {
        final char[] name = pName.toLowerCase().toCharArray();
        name[0] = Character.toUpperCase( name[0] );
        return new String( name );
    }

    public static String clazzName(final File base,
                                   final File file) {
        final int rootLength = base.getAbsolutePath().length();
        final String absFileName = file.getAbsolutePath();
        final int p = absFileName.lastIndexOf('.');
        final String relFileName = absFileName.substring(rootLength + 1, p);
        return relFileName.replace(File.separatorChar, '.');
    }

    public static String relative(final File base,
                                  final File file) {
        final int rootLength = base.getAbsolutePath().length();
        final String absFileName = file.getAbsolutePath();
        return absFileName.substring(rootLength + 1);
    }

    public static String canonicalName(Class clazz) {
        StringBuilder name = new StringBuilder();

        if ( clazz.isArray() ) {
            name.append( canonicalName( clazz.getComponentType() ) );
            name.append( "[]" );
        } else if ( clazz.getDeclaringClass() == null ) {
            name.append( clazz.getName() );
        } else {
            name.append( canonicalName( clazz.getDeclaringClass() ) );
            name.append( "." );
            name.append( clazz.getName().substring( clazz.getDeclaringClass().getName().length() + 1 ) );
        }

        return name.toString();
    }

    /**
     * This method will attempt to load the specified Class. It uses
     * a syncrhonized HashMap to cache the reflection Class lookup.
     */
    public static Class<?> loadClass(String className,
                                     ClassLoader classLoader) {
        Class cls = classes.get(className );
        if ( cls == null ) {
            try {
                cls = Class.forName( className );
            } catch ( Exception e ) {
                //swallow
            }

            //ConfFileFinder
            if ( cls == null && classLoader != null ) {
                try {
                    cls = classLoader.loadClass( className );
                } catch ( Exception e ) {
                    //swallow
                }
            }

            if ( cls == null ) {
                try {
                    cls = ClassUtils.class.getClassLoader().loadClass( className );
                } catch ( Exception e ) {
                    //swallow
                }
            }

            if ( cls == null ) {
                try {
                    cls = Thread.currentThread().getContextClassLoader().loadClass( className );
                } catch ( Exception e ) {
                    //swallow
                }
            }

            if ( cls == null ) {
                try {
                    cls = ClassLoader.getSystemClassLoader().loadClass( className );
                } catch ( Exception e ) {
                    //swallow
                }
            }

            if ( cls != null ) {
                classes.put( className, cls );
            } else {
                throw new RuntimeException( "Unable to load class '" + className + "'" );
            }
        }
        return cls;
    }

    public static Object instantiateObject(String className) {
        return instantiateObject(className,
                (ClassLoader)null);
    }

    /**
     * This method will attempt to create an instance of the specified Class. It uses
     * a syncrhonized HashMap to cache the reflection Class lookup.
     */
    public static Object instantiateObject(String className,
                                           ClassLoader classLoader) {
        Object object;
        try {
            object = loadClass(className, classLoader).newInstance();
        } catch ( Throwable e ) {
            throw new RuntimeException( "Unable to instantiate object for class '" + className + "'",
                                        e );
        }
        return object;
    }

    /**
     * This method will attempt to create an instance of the specified Class. It uses
     * a synchronized HashMap to cache the reflection Class lookup.  It will execute the default
     * constructor with the passed in arguments
     * @param className the name of the class
     * @param args  arguments to default constructor
     */
    public static Object instantiateObject(String className,
                                           ClassLoader classLoader, Object...args) {
        Constructor c = constructors.computeIfAbsent(className, n -> loadClass(n, classLoader).getConstructors()[0]);

        Object object;
        try {
            object = c.newInstance(args);
        } catch ( Throwable e ) {
            throw new RuntimeException( "Unable to instantiate object for class '" + className +
                    "' with constructor " + c, e );
        }
        return object;
    }

    /**
     * This method will attempt to create an instance of the specified Class. It uses
     * a synchronized HashMap to cache the reflection Class lookup.  It will execute the default
     * constructor with the passed in arguments
     * @param className teh name of the class
     * @param args  arguments to default constructor
     */
    public static Object instantiateObject(String className, Object...args) {
        return instantiateObject(className, null, args);
    }

    /**
     * Populates the import style pattern map from give comma delimited string
     */
    public static void addImportStylePatterns(Map<String, Object> patterns,
                                              String str) {
        if ( str == null || "".equals( str.trim() ) ) {
            return;
        }

        String[] items = str.split( " " );
        for (String item : items) {
            String qualifiedNamespace = item.substring(0,
                    item.lastIndexOf('.')).trim();
            String name = item.substring(item.lastIndexOf('.') + 1).trim();
            Object object = patterns.get(qualifiedNamespace);
            if (object == null) {
                if (STAR.equals(name)) {
                    patterns.put(qualifiedNamespace,
                            STAR);
                } else {
                    // create a new list and add it
                    List<String> list = new ArrayList<>();
                    list.add(name);
                    patterns.put(qualifiedNamespace, list);
                }
            } else if (name.equals(STAR)) {
                // if its a STAR now add it anyway, we don't care if it was a STAR or a List before
                patterns.put(qualifiedNamespace, STAR);
            } else {
                // its a list so add it if it doesn't already exist
                List list = (List) object;
                if (!list.contains(name)) {
                    list.add(name);
                }
            }
        }
    }

    /**
     * Determines if a given full qualified class name matches any import style patterns.
     */
    public static boolean isMatched(Map<String, Object> patterns,
                                    String className) {
        // Array [] object class names are "[x", where x is the first letter of the array type
        // -> NO '.' in class name, thus!
        // see http://download.oracle.com/javase/6/docs/api/java/lang/Class.html#getName%28%29
        String qualifiedNamespace = className;
        String name = className;
        if( className.indexOf('.') > 0 ) { 
            qualifiedNamespace = className.substring( 0, className.lastIndexOf( '.' ) ).trim();
            name = className.substring( className.lastIndexOf( '.' ) + 1 ).trim();
        }
        else if( className.indexOf('[') == 0 ) { 
           qualifiedNamespace = className.substring(0, className.lastIndexOf('[') );
        }
        Object object = patterns.get( qualifiedNamespace );
        if ( object == null ) {
            return true;
        } else if ( STAR.equals( object ) ) {
            return false;
        } else if ( patterns.containsKey( "*" ) ) {
            // for now we assume if the name space is * then we have a catchall *.* pattern
            return true;
        } else {
            List list = (List) object;
            return !list.contains( name );
        }
    }

    /**
     * Extracts the package name from the given class object
     */
    public static String getPackage(Class<?> cls) {
        // cls.getPackage() sometimes returns null, in which case fall back to string massaging.
        Package pkg = cls.isArray() ? cls.getComponentType().getPackage() : cls.getPackage();
        if ( pkg == null ) {
            int dotPos;
            int dolPos = cls.getName().indexOf( '$' );
            if ( dolPos > 0 ) {
                // we have nested classes, so adjust dotpos to before first $
                dotPos = cls.getName().substring( 0, dolPos ).lastIndexOf( '.' );
            } else {
                dotPos = cls.getName().lastIndexOf( '.' );
            }
                
            if ( dotPos > 0 ) {
                return cls.getName().substring( 0,
                                                dotPos );
            } else {
                // must be default package.
                return "";
            }
        } else {
            return pkg.getName();
        }
    }

    public static Class<?> findClass(String name, Collection<String> availableImports, ClassLoader cl) {
        Class<?> clazz = null;
        for (String imp : availableImports) {
            if (imp.endsWith(".*")) {
                imp = imp.substring(0, imp.length()-2);
            }
            String className = imp.endsWith(name) ? imp : imp + "." + name;
            clazz = findClass(className, cl);
            if (clazz != null) {
                break;
            }
        }
        return clazz;
    }

    public static Class<?> findClass(String className, ClassLoader cl) {
        try {
            return Class.forName(className, false, cl);
        } catch (ClassNotFoundException e) {
            int lastDot = className.lastIndexOf('.');
            className = className.substring(0, lastDot) + "$" + className.substring(lastDot+1);
            try {
                return Class.forName(className, false, cl);
            } catch (ClassNotFoundException e1) { }
        }
        return null;
    }

    public static List<String> getAccessibleProperties( Class<?> clazz ) {
        Set<PropertyInClass> props = new TreeSet<>();
        for (Method m : clazz.getMethods()) {
            if (m.getParameterTypes().length == 0) {
                String propName = getter2property(m.getName());
                if (propName != null && !propName.equals( "class" )) {
                    props.add( new PropertyInClass( propName, m.getDeclaringClass() ) );
                }
            }

            processModifiesAnnotation(clazz, props, m);
        }

        for (Field f : clazz.getFields()) {
            if ( Modifier.isPublic( f.getModifiers() ) && !Modifier.isStatic( f.getModifiers() ) ) {
                props.add( new PropertyInClass( f.getName(), f.getDeclaringClass() ) );
            }
        }

        List<String> accessibleProperties = new ArrayList<>();
        for ( PropertyInClass setter : props ) {
            accessibleProperties.add(setter.setter);
        }
        return accessibleProperties;
    }

    public static Field getField(Class<?> clazz, String field) {
        try {
            return clazz.getDeclaredField( field );
        } catch (NoSuchFieldException e) {
            return clazz.getSuperclass() != null ? getField(clazz.getSuperclass(), field) : null;
        }
    }

    public static Method getAccessor(Class<?> clazz, String field) {
        return Stream.<Supplier<String>>of( () -> "get" + ucFirst(field), () -> field, () -> "is" + ucFirst(field), () -> "get" + field, () -> "is" + field )
                .map( f -> getMethod(clazz, f.get()) ).filter( Optional::isPresent ).findFirst().flatMap( Function.identity() ).orElse( null );
    }

    public static Method getSetter(Class<?> clazz, String field, Class<?>... parameterTypes) {
        return Stream.<Supplier<String>>of(
                () -> "set" + ucFirst(field),
                () -> field,
                () -> "set" + field
        )
                .map( f -> getMethod(clazz, f.get(), parameterTypes) )
                .filter( Optional::isPresent )
                .findFirst()
                .flatMap( Function.identity() )
                .orElse( null );
    }

    // Don't use this method. You should probably use org.drools.core.util.MethodUtils
    private static Optional<Method> getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return Optional.of( clazz.getMethod(name, parameterTypes) );
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public static String ucFirst(final String s) {
        if (s == null) {
            return null;
        }

        if (s.length() == 1) {
            return s.toUpperCase();
        }

        return Character.isLowerCase(s.charAt( 0 )) ? Character.toUpperCase( s.charAt( 0 ) ) + s.substring( 1 ) : s;
    }

    private static Optional<Method> getMethod(Class<?> clazz, String name) {
        try {
            return Optional.of( clazz.getMethod(name) );
        } catch (NoSuchMethodException e) {
            return Optional.empty();
        }
    }

    public static Class extractGenericType(Class<?> clazz, final String methodName) {
        Method method = ClassUtils.getAccessor(clazz, methodName);
        if (method == null) {
            throw new RuntimeException(String.format("Unknown accessor %s on %s", methodName, clazz));
        }

        java.lang.reflect.Type returnType = method.getGenericReturnType();

        if(returnType instanceof ParameterizedType){
            ParameterizedType type = (ParameterizedType) returnType;
            java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                return (Class) typeArguments[0];
            }
        }
        throw new RuntimeException("No generic type");
    }

    private static void processModifiesAnnotation( Class<?> clazz, Set<PropertyInClass> props, Method m ) {
        Modifies modifies = m.getAnnotation( Modifies.class );
        if (modifies != null) {
            for (String prop : modifies.value()) {
                prop = prop.trim();
                try {
                    Field field = clazz.getField(prop);
                    props.add( new PropertyInClass( field.getName(), field.getDeclaringClass() ) );
                } catch (NoSuchFieldException e) {
                    String getter = "get" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                    try {
                        Method method = clazz.getMethod(getter);
                        props.add( new PropertyInClass( prop, method.getDeclaringClass() ) );
                    } catch (NoSuchMethodException e1) {
                        getter = "is" + prop.substring(0, 1).toUpperCase() + prop.substring(1);
                        try {
                            Method method = clazz.getMethod(getter);
                            props.add( new PropertyInClass( prop, method.getDeclaringClass() ) );
                        } catch (NoSuchMethodException e2) {
                            throw new RuntimeException(e2);
                        }
                    }
                }
            }
        }
    }

    public static boolean isTypeCompatibleWithArgumentType( Class<?> actual, Class<?> formal ) {
        if ( actual.isPrimitive() && formal.isPrimitive() ) {
            return isConvertible( actual, formal );
        } else if ( actual.isPrimitive() ) {
            return isConvertible( actual, convertToPrimitiveType( formal ) );
        } else if ( formal.isPrimitive() ) {
            return isConvertible( convertToPrimitiveType( actual ), formal );
        } else {
            return formal.isAssignableFrom( actual );
        }
    }

    public static boolean isAssignable( Class<?> type, Object obj ) {
        return type.isInstance( obj ) || (type.isPrimitive() && convertFromPrimitiveType( type ).isInstance( obj ));
    }

    public static boolean isConvertible( Class<?> srcPrimitive, Class<?> tgtPrimitive ) {
        if ( Boolean.TYPE.equals( srcPrimitive ) ) {
            return Boolean.TYPE.equals( tgtPrimitive );
        } else if ( Byte.TYPE.equals( srcPrimitive ) ) {
            return Byte.TYPE.equals( tgtPrimitive )
                   || Short.TYPE.equals( tgtPrimitive )
                   || Integer.TYPE.equals( tgtPrimitive )
                   || Long.TYPE.equals( tgtPrimitive )
                   || Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        } else if ( Character.TYPE.equals( srcPrimitive ) ) {
            return Character.TYPE.equals( tgtPrimitive )
                   || Integer.TYPE.equals( tgtPrimitive )
                   || Long.TYPE.equals( tgtPrimitive )
                   || Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        } else if ( Double.TYPE.equals( srcPrimitive ) ) {
            return Double.TYPE.equals( tgtPrimitive );
        } else if ( Float.TYPE.equals( srcPrimitive ) ) {
            return Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        } else if ( Integer.TYPE.equals( srcPrimitive ) ) {
            return Integer.TYPE.equals( tgtPrimitive )
                   || Long.TYPE.equals( tgtPrimitive )
                   || Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        } else if ( Long.TYPE.equals( srcPrimitive ) ) {
            return Long.TYPE.equals( tgtPrimitive )
                   || Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        } else if ( Short.TYPE.equals( srcPrimitive ) ) {
            return Short.TYPE.equals( tgtPrimitive )
                   || Integer.TYPE.equals( tgtPrimitive )
                   || Long.TYPE.equals( tgtPrimitive )
                   || Float.TYPE.equals( tgtPrimitive )
                   || Double.TYPE.equals( tgtPrimitive );
        }
        return false;
    }

    public static boolean isIterable(Class<?> clazz) {
        return Iterable.class.isAssignableFrom( clazz ) || clazz.isArray();
    }

    public static boolean isFinal(Class<?> clazz) {
        return Modifier.isFinal( clazz.getModifiers() );
    }
    
    public static boolean isInterface(Class<?> clazz) {
        return Modifier.isInterface( clazz.getModifiers() );
    }

    private static class PropertyInClass implements Comparable {
        private final String setter;
        private final Class<?> clazz;

        private PropertyInClass( String setter, Class<?> clazz ) {
            this.setter = setter;
            this.clazz = clazz;
        }

        public int compareTo(Object o) {
            PropertyInClass other = (PropertyInClass) o;
            if (clazz == other.clazz) {
                return setter.compareTo(other.setter);
            }
            return clazz.isAssignableFrom(other.clazz) ? -1 : 1;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PropertyInClass)) {
                return false;
            }
            PropertyInClass other = (PropertyInClass) obj;
            return clazz == other.clazz && setter.equals(other.setter);
        }

        @Override
        public int hashCode() {
            return 29 * clazz.hashCode() + 31 * setter.hashCode();
        }
    }

    public static String getter2property(String methodName) {
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
        }
        return null;
    }

    public static String setter2property(String methodName) {
        if (!methodName.startsWith("set") || methodName.length() < 4) {
            return null;
        }
        return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
    }

    public static Class<?> convertFromPrimitiveType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        throw new RuntimeException("Class not convertible from primitive: " + type.getName());
    }

    public static Class<?> convertToPrimitiveType(Class<?> type) {
        if (type.isPrimitive()) {
            return type;
        }
        if (type == Integer.class) {
            return int.class;
        }
        if (type == Long.class) {
            return long.class;
        }
        if (type == Float.class) {
            return float.class;
        }
        if (type == Double.class) {
            return double.class;
        }
        if (type == Short.class) {
            return short.class;
        }
        if (type == Byte.class) {
            return byte.class;
        }
        if (type == Character.class) {
            return char.class;
        }
        if (type == Boolean.class) {
            return boolean.class;
        }
        if (type == BigInteger.class) {
            return long.class;
        }
        if (type == BigDecimal.class) {
            return double.class;
        }
        if (type == Number.class) {
            return double.class;
        }
        throw new RuntimeException("Class not convertible to primitive: " + type.getName());
    }

    public static Class<?> convertPrimitiveNameToType( String typeName ) {
        return primitiveNameToType.get(typeName);
    }

    public static Set<Class<?>> getAllImplementedInterfaceNames( Class<?> klass ) {
        Set<Class<?>> interfaces = new HashSet<>();
        while( klass != null ) {
            Class<?>[] localInterfaces = klass.getInterfaces();
            for ( Class<?> intf : localInterfaces ) {
                interfaces.add( intf );
                exploreSuperInterfaces( intf, interfaces );
            }
            klass = klass.getSuperclass();
        }
        return interfaces;
    }

    private static void exploreSuperInterfaces( Class<?> intf, Set<Class<?>> traitInterfaces ) {
        for ( Class<?> sup : intf.getInterfaces() ) {
            traitInterfaces.add( sup );
            exploreSuperInterfaces( sup, traitInterfaces );
        }
    }

    public static Set<Class<?>> getMinimalImplementedInterfaceNames( Class<?> klass ) {
        Set<Class<?>> interfaces = new HashSet<>();
        while( klass != null ) {
            Class<?>[] localInterfaces = klass.getInterfaces();
            for ( Class<?> intf : localInterfaces ) {
                boolean subsumed = false;
                for ( Class<?> i : new ArrayList<>(interfaces) ) {
                    if ( intf.isAssignableFrom( i ) ) {
                        subsumed = true;
                        break;
                    } else if ( i.isAssignableFrom( intf ) ) {
                        interfaces.remove( i );
                    }
                }
                if ( subsumed ) {
                    continue;
                }
                interfaces.add( intf );
            }
            klass = klass.getSuperclass();
        }
        return interfaces;
    }

    public static boolean isCaseSenstiveOS() {
        String os =  System.getProperty("os.name").toUpperCase();
        return os.contains( "WINDOWS" ) || os.contains( "MAC OS X" );
    }

    public static boolean isWindows() {
        String os =  System.getProperty("os.name");
        return os.toUpperCase().contains( "WINDOWS" );
    }

    public static boolean isOSX() {
        String os =  System.getProperty("os.name");
        return os.toUpperCase().contains( "MAC OS X" );
    }

    public static boolean isJboss() {
        return System.getProperty("jboss.server.name") != null;
    }

    /**
     * Checks if running on Android operating system
     */
    public static boolean isAndroid() {
        return IS_ANDROID;
    }

    public static Class<?> findCommonSuperClass(Class<?> c1, Class<?> c2) {
        if (c1 == null) {
            return c2;
        }
        if (c2 == null) {
            return c1;
        }
        if (c1.isAssignableFrom( c2 )) {
            return c1;
        }
        if (c2.isAssignableFrom( c1 )) {
            return c2;
        }
        for (Class<?> parent = c1.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
            if (parent.isAssignableFrom(c2)) {
                return parent;
            }
        }
        return c1;
    }

    public static Class<?> getClassFromName(String className) throws ClassNotFoundException {
        return getClassFromName( className, true, ClassUtils.class.getClassLoader() );
    }

    public static Class<?> getClassFromName(String className, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            Class<?> clazz;
            if (abbreviationMap.containsKey(className)) {
                final String clsName = "[" + abbreviationMap.get(className);
                clazz = Class.forName(clsName, initialize, classLoader).getComponentType();
            } else {
                clazz = Class.forName(toCanonicalName( className ), initialize, classLoader);
            }
            return clazz;
        } catch (final ClassNotFoundException ex) {
            // allow path separators (.) as inner class name separators
            final int lastDotIndex = className.lastIndexOf('.');

            if (lastDotIndex != -1) {
                try {
                    return getClassFromName( className.substring( 0, lastDotIndex ) + '$' + className.substring( lastDotIndex + 1 ),
                                             initialize, classLoader);
                } catch (final ClassNotFoundException ex2) { // NOPMD
                    // ignore exception
                }
            }

            throw ex;
        }
    }

    private static String toCanonicalName(String className) {
        if (className == null) {
            throw new NullPointerException("className must not be null.");
        } else if (className.endsWith("[]")) {
            final StringBuilder classNameBuffer = new StringBuilder();
            while (className.endsWith("[]")) {
                className = className.substring(0, className.length() - 2);
                classNameBuffer.append("[");
            }
            final String abbreviation = abbreviationMap.get(className);
            if (abbreviation != null) {
                classNameBuffer.append(abbreviation);
            } else {
                classNameBuffer.append("L").append(className).append(";");
            }
            className = classNameBuffer.toString();
        }
        return className;
    }

    public static Class<?> safeLoadClass(ClassLoader cl, String name) {
        try {
            return cl.loadClass( name );
        }
        catch ( final ClassNotFoundException | NoClassDefFoundError cnfe ) {
            // class doesn't exist
            // potential mis-match induced by Mac/OSX
        }
        return null;
    }

    public static String getSimpleName(Class<?> c) {
        return getCanonicalSimpleName( c, '$' );
    }

    public static String getCanonicalSimpleName(Class<?> c) {
        return getCanonicalSimpleName( c, '.' );
    }

    public static String getCanonicalSimpleName(Class<?> c, char separator) {
        Class<?> enclosingClass = c.getEnclosingClass();
        return enclosingClass != null ?
               getCanonicalSimpleName(enclosingClass) + separator + c.getSimpleName() :
               c.getSimpleName();
    }
}
