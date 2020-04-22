package org.drools.core.factmodel.traits;

import org.drools.core.util.HierarchyEncoder;

public interface TraitRegistry {

    HierarchyEncoder<String> getHierarchy();

    void merge(TraitRegistry other);
}
