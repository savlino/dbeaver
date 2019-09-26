/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskConfigPanel;
import org.jkiss.dbeaver.model.task.DBTTaskConfigurator;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseObjectsSelectorPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data transfer task configurator
 */
public class DataTransferTaskConfigurator implements DBTTaskConfigurator {

    @Override
    public ConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, @NotNull DBTTaskType taskType) {
        return new ConfigPanel(runnableContext, taskType);
    }

    @Override
    public IWizard createTaskConfigWizard(@NotNull DBTTask taskConfiguration) {
        return new DataTransferWizard(UIUtils.getDefaultRunnableContext(), taskConfiguration);
    }

    private static class ConfigPanel implements DBTTaskConfigPanel {

        private DBRRunnableContext runnableContext;
        private DBTTaskType taskType;
        private DatabaseObjectsSelectorPanel selectorPanel;

        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            this.runnableContext = runnableContext;
            this.taskType = taskType;
        }

        @Override
        public void createControl(Object parent, IPropertyChangeListener propertyChangeListener) {
            Group group = UIUtils.createControlGroup(
                (Composite) parent,
                (DTConstants.TASK_EXPORT.equals(taskType.getId()) ? "Export tables" : "Import into"),
                1,
                GridData.FILL_BOTH,
                0);
            selectorPanel = new DatabaseObjectsSelectorPanel(group, runnableContext);
            selectorPanel.addSelectionListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    propertyChangeListener.propertyChange(new PropertyChangeEvent(selectorPanel, "nodes", null, null));
                }
            });
        }

        @Override
        public void loadSettings(DBRRunnableContext runnableContext, Map<String, Object> configuration) {

        }

        @Override
        public void saveSettings(DBRRunnableContext runnableContext, Map<String, Object> configuration) {
            List<DBNNode> checkedNodes = selectorPanel.getCheckedNodes();
            List<IDataTransferNode> nodes = new ArrayList<>();
            boolean isExport = taskType.getId().equals(DTConstants.TASK_EXPORT);
            String nodeType = isExport ? "producers" : "consumers";
            for (DBNNode node : checkedNodes) {
                if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    if (object instanceof DBSObjectContainer) {
/*
                        runnableContext.run(true, true, monitor -> {
                            ((DBSObjectContainer) object).getChildren(monitor)
                        });
*/
                    }
                    addNode(nodes, object, isExport);
                }
            }
            DataTransferWizard.saveNodesLocation(configuration, nodes, nodeType);
        }

        private void addNode(List<IDataTransferNode> nodes, DBSObject object, boolean isExport) {
            if (isExport) {
                if (object instanceof DBSDataContainer) {
                    nodes.add(new DatabaseTransferProducer((DBSDataContainer) object));
                }
            } else {
                if (object instanceof DBSDataManipulator) {
                    nodes.add(new DatabaseTransferConsumer((DBSDataManipulator) object));
                }
            }
        }

        @Override
        public boolean isComplete() {
            return !selectorPanel.getCheckedNodes().isEmpty();
        }
    }

}