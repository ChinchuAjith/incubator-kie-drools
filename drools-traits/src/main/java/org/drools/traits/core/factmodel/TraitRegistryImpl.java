/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.traits.core.factmodel;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.base.factmodel.ClassDefinition;
import org.drools.base.factmodel.FieldDefinition;
import org.drools.base.factmodel.traits.CoreWrapper;
import org.drools.base.factmodel.traits.Thing;
import org.drools.base.factmodel.traits.Trait;
import org.drools.base.factmodel.traits.TraitableBean;
import org.drools.base.rule.TypeDeclaration;
import org.drools.util.ClassUtils;
import org.drools.traits.core.util.HierNode;
import org.drools.traits.core.util.HierarchyEncoderImpl;
import org.kie.api.definition.type.FactField;

public class TraitRegistryImpl implements Externalizable,
                                          TraitRegistry {


    private Map<String, ClassDefinition> traits;
    private Map<String, ClassDefinition> traitables;
    private Map<String, Set<String>> staticTraitTypes;

    private int codeSize = 0;

    private Map<String, BitSet> masks;

    private HierarchyEncoder<String> hierarchy;


    public TraitRegistryImpl() {
        init();
    }

    private void init() {
        TypeDeclaration thingType = new TypeDeclaration( Thing.class.getName() );
        thingType.setKind( TypeDeclaration.Kind.TRAIT );
        thingType.setTypeClass( Thing.class );
        ClassDefinition def = new ClassDefinition();
        def.setClassName( thingType.getTypeClass().getName() );
        def.setDefinedClass( Thing.class );
        addTrait( def );

        ClassDefinition individualDef = new ClassDefinition();
        individualDef.setClassName( Entity.class.getName() );
        individualDef.setDefinedClass( Entity.class );
        individualDef.setInterfaces( new String[]{ Serializable.class.getName(), TraitableBean.class.getName() } );
        individualDef.setTraitable( true );
        addTraitable( individualDef );

        ClassDefinition mapcoreDef = new ClassDefinition();
        mapcoreDef.setClassName( MapCore.class.getName() );
        mapcoreDef.setDefinedClass( MapCore.class );
        mapcoreDef.setInterfaces( new String[] { Serializable.class.getName(), TraitableBean.class.getName(), CoreWrapper.class.getName() } );
        mapcoreDef.setTraitable( true );
        addTraitable( mapcoreDef );

        ClassDefinition logicalMapcoreDef = new ClassDefinition();
        logicalMapcoreDef.setClassName( LogicalMapCore.class.getName() );
        logicalMapcoreDef.setDefinedClass( LogicalMapCore.class );
        logicalMapcoreDef.setInterfaces( new String[] { Serializable.class.getName(), TraitableBean.class.getName(), CoreWrapper.class.getName() } );
        logicalMapcoreDef.setTraitable( true, true );
        addTraitable( logicalMapcoreDef );
    }

    public void merge( TraitRegistry otherRegistry ) {
        TraitRegistryImpl other = (TraitRegistryImpl) otherRegistry;
        if ( staticTraitTypes == null && other.staticTraitTypes != null ) {
            staticTraitTypes = new HashMap<>();
            staticTraitTypes.putAll( other.staticTraitTypes );
        }

        if ( traits == null ) {
            traits = new HashMap<>();
        }
        if ( other.traits != null ) {
            this.traits.putAll( other.traits );
        }

        if ( traitables == null ) {
            traitables = new HashMap<>();
        }
        if ( other.traitables != null ) {
            this.traitables.putAll( other.traitables );
        }

        if ( masks == null ) {
            masks = new HashMap<>();
        }
        if ( other.masks != null ) {
            this.masks.putAll( other.masks );
        }

        if ( hierarchy == null || hierarchy.size() <= 1 ) {
            hierarchy = other.hierarchy;
        } else if ( other.traits != null ) {
            hierarchy = mergeHierarchy( other, this );
        }
    }

    private static HierarchyEncoder<String> mergeHierarchy(TraitRegistryImpl first, TraitRegistryImpl second ) {
        for ( String traitName : second.getHierarchy().getSortedMembers() ) {
            ClassDefinition trait = second.traits.get( traitName );
            List<String> parentTraits = new ArrayList<>( );
            for ( String candidateIntf : trait.getInterfaces() ) {
                if ( first.getHierarchy().getCode( candidateIntf ) != null ) {
                    parentTraits.add( candidateIntf );
                }
            }
            first.getHierarchy().encode( traitName, parentTraits );
        }
        return first.getHierarchy();
    }

    public Map<String, ClassDefinition> getTraits() {
        return traits;
    }

    protected ClassDefinition getTrait( String key ) {
        if ( key.endsWith(TraitFactoryImpl.SUFFIX ) ) {
            key = key.replace(TraitFactoryImpl.SUFFIX , "" );
        }
        ClassDefinition traitDef = traits != null ? traits.get( key ) : null;
        if ( traitDef == null ) {

        }
        return traitDef;
    }

    public Map<String, ClassDefinition> getTraitables() {
        return traitables;
    }

    protected ClassDefinition getTraitable( String key ) {
        return traitables != null ? traitables.get( key ) : null;
    }


    public void addTrait( ClassDefinition trait ) {
        addTrait( trait.getClassName(), trait );
    }


    public void addTrait( String className, ClassDefinition trait ) {
        if ( traits == null ) {
            traits = new HashMap<>();
        }
        this.traits.put( className, trait );
        getHierarchy().encode( className, getTraitInterfaces( trait ) );
    }

    private Collection<String> getTraitInterfaces( ClassDefinition trait ) {
        List<String> intfs = new ArrayList<>();
        for ( String s : trait.getInterfaces() ) {
            if ( traits.containsKey( s ) ) {
                intfs.add( s );
            }
        }
        return intfs;
    }

    public void addTraitable( ClassDefinition traitable ) {
        if ( traitables == null ) {
            traitables = new HashMap<>();
        }
        this.traitables.put( traitable.getClassName(), traitable );
        Set<String> staticTraits = detectStaticallyImplementedTraits( traitable );
        if ( ! staticTraits.isEmpty() ) {
            if ( staticTraitTypes == null ) {
                staticTraitTypes = new HashMap<>();
            }
            staticTraitTypes.put( traitable.getClassName(), staticTraits );
        }
    }


    public static boolean isSoftField( FieldDefinition field, int index, BitSet mask ) {
        return ! mask.get( index );
    }

    public BitSet getFieldMask( String trait, String traitable ) {
        if ( masks == null ) {
            masks = new HashMap<>();
        }
        String key = trait + traitable;
        return masks.computeIfAbsent( key, k -> bind( trait, traitable ) );
    }

    private BitSet bind( String trait, String traitable ) throws UnsupportedOperationException {
        ClassDefinition traitDef = getTrait( trait );
        if ( traitDef == null ) {
            throw new UnsupportedOperationException( " Unable to apply trait " + trait + " to class " + traitable + " : not a trait " );
        }
        ClassDefinition traitableDef = getTraitable( traitable );
        if ( traitableDef == null ) {
            throw new UnsupportedOperationException( " Unable to apply trait " + trait + " to class " + traitable + " : not a traitable " );
        }

        int j = 0;
        BitSet bitmask = new BitSet( traitDef.getFields().size() );
        for ( FactField field : traitDef.getFields() ) {
            String alias = ((FieldDefinition) field).resolveAlias();

            FieldDefinition concreteField = traitableDef.getFieldByAlias( alias );

            if ( concreteField != null ) {
                if ( ! traitableDef.isFullTraiting() && ! concreteField.getType().isAssignableFrom( field.getType() ) ) {
                    throw new UnsupportedOperationException( " Unable to apply trait " + trait + " to class " + traitable + " :" +
                                                             " trait field " + field.getName() + ":" + ( (FieldDefinition) field ).getTypeName() + " is incompatible with" +
                                                             " concrete hard field " + concreteField.getName() + ":" + concreteField.getTypeName() + ". Consider enabling logical traiting" +
                                                             " mode using @Traitable( logical = true )" );
                }

                bitmask.set( j );
            }
            j++;
        }

        return bitmask;
    }


    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        objectOutput.writeObject( traits );
        objectOutput.writeObject( traitables );
        objectOutput.writeObject( masks );
        objectOutput.writeObject( hierarchy );
        objectOutput.writeInt( codeSize );
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        traits = (Map<String, ClassDefinition>) objectInput.readObject();
        traitables = (Map<String, ClassDefinition>) objectInput.readObject();
        masks = (Map<String, BitSet>) objectInput.readObject();
        hierarchy = (HierarchyEncoderImpl) objectInput.readObject();
        codeSize = objectInput.readInt();
        init();
    }


    public HierarchyEncoder<String> getHierarchy() {
        if ( hierarchy == null ) {
            hierarchy = new CachingHierarcyEncoderImpl();
        }
        return hierarchy;
    }

    protected Set<String> detectStaticallyImplementedTraits( ClassDefinition traitable ) {
        Set<String> traitInterfaces = new HashSet<>( 3 );
        for ( Class<?> intf : ClassUtils.getAllImplementedInterfaceNames( traitable.getDefinedClass() ) ) {
            if ( Thing.class.isAssignableFrom( intf ) || intf.getAnnotation( Trait.class ) != null ) {
                traitInterfaces.add( intf.getName() );
            }
        }
        return traitInterfaces;
    }

    public BitSet getStaticTypeCode( String className ) {
        if ( staticTraitTypes != null && staticTraitTypes.containsKey( className ) ) {
            CachingHierarcyEncoderImpl cachingHierarcyEncoder = (CachingHierarcyEncoderImpl) hierarchy;
            if ( cachingHierarcyEncoder.hasCodeForClass( className ) ) {
                return cachingHierarcyEncoder.getCodeForClass( className );
            } else {
                return cachingHierarcyEncoder.cacheAndGetCode( className, staticTraitTypes.get( className ) );
            }
        } else {
            return null;
        }
    }

    public Set<String> getStaticTypes( String name ) {
        return staticTraitTypes.get( name );
    }

    public static class CachingHierarcyEncoderImpl extends HierarchyEncoderImpl<String> {

        private Map<String,BitSet> cache;

        @Override
        protected void encode( HierNode<String> node ) {
            super.encode( node );
            invalidateCache();
        }

        private void invalidateCache() {
            if ( cache != null ) {
                cache.clear();
            }
        }

        public boolean hasCodeForClass( String className ) {
            return cache != null && cache.containsKey( className );
        }

        public BitSet getCodeForClass( String className ) {
            return cache.get( className );
        }

        public BitSet cacheAndGetCode( String className, Set<String> parents ) {
            BitSet bitSet = new BitSet( this.getBottom().length() );
            for ( String parent : parents ) {
                bitSet.or( getCode( parent ) );
            }
            if ( cache == null ) {
                cache = new HashMap<>();
            }
            cache.put( className, bitSet );
            return bitSet;
        }
    }
}


