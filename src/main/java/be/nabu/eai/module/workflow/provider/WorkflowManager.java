/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.workflow.provider;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.validation.constraints.NotNull;

import be.nabu.libs.types.api.KeyValuePair;
import nabu.misc.workflow.types.WorkflowBatchInstance;
import nabu.misc.workflow.types.WorkflowDefinition;
import nabu.misc.workflow.types.WorkflowInstance;
import nabu.misc.workflow.types.WorkflowInstance.Level;
import nabu.misc.workflow.types.WorkflowInstanceProperty;
import nabu.misc.workflow.types.WorkflowTransitionInstance;

public interface WorkflowManager {
	public void createWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowInstance instance);
	public void updateWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowInstance instance);
	@WebResult(name = "workflow")
	public WorkflowInstance getWorkflow(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull UUID workflowId);
	
	@WebResult(name = "workflows")
	public List<WorkflowInstance> getWorkflows(
		@WebParam(name = "connectionId") String connectionId, 
		@NotNull @WebParam(name = "definitionId") String definitionId, 
		@WebParam(name = "stateId") UUID stateId, 
		@WebParam(name = "transitionState") Level state, 
		@WebParam(name = "from") Date from, 
		@WebParam(name = "until") Date until, 
		@WebParam(name = "environment") String environment, 
		@WebParam(name = "parentId") UUID parentId, 
		@WebParam(name = "batchId") UUID batchId, 
		@WebParam(name = "correlationId") String correlationId,
		@WebParam(name = "contextId") String contextId,
		@WebParam(name = "groupId") String groupId,
		@WebParam(name = "workflowType") String workflowType,
		@WebParam(name = "properties") List<KeyValuePair> properties, 
		@WebParam(name = "offset") Integer offset, 
		@WebParam(name = "limit") Integer limit,
		// whether or not to limit yourself to "running" workflows (in non-final states)
		@WebParam(name = "running") Boolean running);
	
	public void createWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	public void updateWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "properties") List<WorkflowInstanceProperty> properties);
	@WebResult(name = "properties")
	public List<WorkflowInstanceProperty> getWorkflowProperties(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull UUID workflowId);
	
	public void createTransition(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	public void updateTransition(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @WebParam(name = "instance") WorkflowTransitionInstance instance);
	@WebResult(name = "transitions")
	public List<WorkflowTransitionInstance> getTransitions(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "workflowId") @NotNull UUID workflowId);

	public void createBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowBatchInstance instance);
	// update the batch but do so in a way that it can not fail in a concurrent environment
	// for example only select for update or only update if the state etc is still the same
	@WebResult(name = "succeeded")
	public boolean updateBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "transactionId") String transactionId, @NotNull @WebParam(name = "instance") WorkflowBatchInstance instance);
	// get the current batch state, this should be:
	// RUNNING: until the "created" is filled in and all child workflows are created
	// WAITING: when all child workflows are created and we are waiting for them to be completed
	// STOPPED: when all child workflows are completed but the batch is not yet progressing (note that this is a computed state only, the batch itself will not appear in this state in the database)
	// ERROR/FAILED: at least one of the child workflows is in error (we don't continue with batch)
	// SUCCEEDED: the batch is done and all child workflows are correct
	public Level calculateBatchState(@WebParam(name = "connectionId") String connectionId, @NotNull @WebParam(name = "batchId") UUID batchId);
	@WebResult(name = "batch")
	public WorkflowBatchInstance getBatch(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "batchId") @NotNull UUID batchId);
	
	@WebResult(name = "batches")
	public List<WorkflowBatchInstance> getBatches(@WebParam(name = "connectionId") String connectionId, @WebParam(name = "state") Level state, @WebParam(name = "offset") Integer offset, @WebParam(name = "limit") Integer limit);

	// whenever a workflow is created, we need to make sure the definition exists in the database for historical purposes
	// you can obviously ignore this if you don't care about versions
	public void mergeDefinition(@WebParam(name = "definition") WorkflowDefinition definition);
	// get the definition of a specific version (if no version specified: the latest)
	@WebResult(name = "definition")
	public WorkflowDefinition getDefinition(@NotNull @WebParam(name = "definitionId") String workflowId, @WebParam(name = "version") Long version);
	
	@WebResult(name = "amount")
	public default Long getAmountOfWorkflows(
		@WebParam(name = "connectionId") String connectionId, 
		@NotNull @WebParam(name = "definitionId") String definitionId, 
		@WebParam(name = "stateId") UUID stateId, 
		@WebParam(name = "transitionState") Level state, 
		@WebParam(name = "from") Date from, 
		@WebParam(name = "until") Date until, 
		@WebParam(name = "environment") String environment, 
		@WebParam(name = "parentId") UUID parentId, 
		@WebParam(name = "batchId") UUID batchId, 
		@WebParam(name = "correlationId") String correlationId,
		@WebParam(name = "contextId") String contextId,
		@WebParam(name = "groupId") String groupId,
		@WebParam(name = "workflowType") String workflowType,
		@WebParam(name = "properties") List<KeyValuePair> properties, 
		// whether or not to limit yourself to "running" workflows (in non-final states)
		@WebParam(name = "running") Boolean running) {
		
		return (long) getWorkflows(connectionId, definitionId, stateId, state, from, until, environment, parentId, batchId, correlationId, contextId, groupId, workflowType, properties, null, null, running).size();
	}
}
