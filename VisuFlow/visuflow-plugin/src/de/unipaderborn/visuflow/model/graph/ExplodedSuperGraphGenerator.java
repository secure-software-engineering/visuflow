package de.unipaderborn.visuflow.model.graph;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

import de.unipaderborn.visuflow.model.DataModel;
import de.unipaderborn.visuflow.model.VFClass;
import de.unipaderborn.visuflow.model.VFMethod;
import de.unipaderborn.visuflow.model.VFUnit;
import de.unipaderborn.visuflow.ui.graph.EsgLayout;
import de.unipaderborn.visuflow.util.ServiceUtil;
import soot.Unit;

public class ExplodedSuperGraphGenerator {

	public void clearEsgSets() {
		DataModel data = ServiceUtil.getService(DataModel.class);
		List<VFClass> classList = data.listClasses();
		if(classList != null) {
			for (VFClass vfClass : classList) {
				for (VFMethod vfMethod : vfClass.getMethods()) {
					for (VFUnit vfUnit : vfMethod.getUnits()) {
						vfUnit.clearESGSets();
					}
				}
			}
		}
	}
}
