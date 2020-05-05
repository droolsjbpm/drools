package org.drools.core.factmodel.traits;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public interface TraitableBean<K, X extends TraitableBean> {

    String MAP_FIELD_NAME = "__$$dynamic_properties_map$$";
    String TRAITSET_FIELD_NAME = "__$$dynamic_traits_map$$";
    String FIELDTMS_FIELD_NAME = "__$$field_Tms$$";

    Map<String,Object> _getDynamicProperties();

    void _setDynamicProperties( Map<String,Object> map );

    Map<String,Thing<K>> _getTraitMap();

    void _setTraitMap( Map<String,Thing<K>> map );


    TraitFieldTMS _getFieldTMS();

    void _setFieldTMS( TraitFieldTMS traitFieldTMS );


    default void addTrait(String type, Thing proxy) throws LogicalTypeInconsistencyException {
        ((ITraitTypeMap) _getTraitMap()).putSafe(type, proxy);
    }

    default Thing<K> getTrait(String type) {
        return _getTraitMap().get( type );
    }

    default boolean hasTrait(String type) {
        return isTraitMapInitialized() && _getTraitMap().containsKey(type);
    }

    default boolean hasTraits() {
        return _getTraitMap() != null && ! _getTraitMap().isEmpty();
    }

    default Collection<Thing<K>> removeTrait( String type ) {
        if ( isTraitMapInitialized() ) {
            return ((ITraitTypeMap)_getTraitMap()).removeCascade(type);
        } else {
            return null;
        }
    }

    default Collection<Thing<K>> removeTrait( BitSet typeCode ) {
        if ( isTraitMapInitialized() ) {
            return ((ITraitTypeMap)_getTraitMap()).removeCascade( typeCode );
        } else {
            return null;
        }
    }

    default Collection<String> getTraits() {
        if ( isTraitMapInitialized() ) {
            return _getTraitMap().keySet();
        } else {
            return Collections.emptySet();
        }
    }

    default Collection<Thing<K>> getMostSpecificTraits() {
        if ( _getTraitMap() == null ) {
            return Collections.emptyList();
        }
        return ((ITraitTypeMap) _getTraitMap()).getMostSpecificTraits();
    }

    default BitSet getCurrentTypeCode() {
        if ( _getTraitMap() == null ) {
            return null;
        }
        return ((ITraitTypeMap) _getTraitMap()).getCurrentTypeCode();
    }

    default boolean isTraitMapInitialized() {
        return _getTraitMap() != null;
    }


    default void _setBottomTypeCode( BitSet bottomTypeCode ) {
        ((ITraitTypeMap) _getTraitMap()).setBottomCode( bottomTypeCode );
    }

}
