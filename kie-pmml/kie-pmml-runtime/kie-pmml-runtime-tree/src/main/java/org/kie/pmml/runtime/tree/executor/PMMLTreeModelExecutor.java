package org.kie.pmml.runtime.tree.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.api.KieServices;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.ParameterInfo;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.pmml.api.exceptions.KiePMMLException;
import org.kie.pmml.api.model.KiePMMLModel;
import org.kie.pmml.api.model.enums.PMML_MODEL;
import org.kie.pmml.api.model.tree.KiePMMLTreeModel;
import org.kie.pmml.runtime.api.exceptions.KiePMMLModelException;
import org.kie.pmml.runtime.api.executor.PMMLContext;
import org.kie.pmml.runtime.core.executor.PMMLModelExecutor;

public class PMMLTreeModelExecutor implements PMMLModelExecutor {

    private final KieServices kieServices;
    private final KieContainer kContainer;

    public PMMLTreeModelExecutor() {
        this.kieServices = KieServices.Factory.get();
        // TODO {gcardosi} is this correct?
        this.kContainer = kieServices.getKieClasspathContainer();
    }

    @Override
    public PMML_MODEL getPMMLModelType() {
        return PMML_MODEL.TREE_MODEL;
    }

    @Override
    public PMML4Result evaluate(KiePMMLModel model, PMMLContext context) throws KiePMMLException {
        if (!(model instanceof KiePMMLTreeModel)) {
            throw new KiePMMLModelException("Expected a KiePMMLTreeModel, received a " + model.getClass().getName());
        }
        final KiePMMLTreeModel treeModel = (KiePMMLTreeModel) model;
        PMML4Result toReturn = new PMML4Result();
        StatelessKieSession kSession = kContainer.newStatelessKieSession("PMMLTreeModelSession");
        Map<String, Object> unwrappedInputParams = getUnwrappedParametersMap(context.getRequestData().getMappedRequestParams());
        List<Object> executionParams = new ArrayList<>();
        executionParams.add(treeModel);
        executionParams.add(toReturn);
        executionParams.add(unwrappedInputParams);
        kSession.execute(executionParams);
        return toReturn;
    }

    private Map<String, Object> getUnwrappedParametersMap(Map<String, ParameterInfo> parameterMap) {
       return parameterMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().getValue()));

    }


}
