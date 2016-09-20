package nabu.misc.workflow.types;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = { "id", "definitionId", "parentId", "batchId", "contextId", "started", "stopped", "environment", "transitionState", "stateId" })
public class WorkflowInstance {
	private String id, parentId, definitionId;
	private Date started, stopped;
	private String stateId, batchId, contextId, environment;
	private Level transitionState;
	
	@NotNull
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	@NotNull
	public String getDefinitionId() {
		return definitionId;
	}
	public void setDefinitionId(String definitionId) {
		this.definitionId = definitionId;
	}
	
	@NotNull
	public Date getStarted() {
		return started;
	}
	public void setStarted(Date started) {
		this.started = started;
	}
	
	public Date getStopped() {
		return stopped;
	}
	public void setStopped(Date stopped) {
		this.stopped = stopped;
	}
	
	@NotNull
	public String getStateId() {
		return stateId;
	}
	public void setStateId(String stateId) {
		this.stateId = stateId;
	}
	
	public String getBatchId() {
		return batchId;
	}
	public void setBatchId(String batchId) {
		this.batchId = batchId;
	}
	
	public String getContextId() {
		return contextId;
	}
	public void setContextId(String contextId) {
		this.contextId = contextId;
	}
	
	@NotNull
	public String getEnvironment() {
		return environment;
	}
	public void setEnvironment(String environment) {
		this.environment = environment;
	}
	
	@NotNull	
	public Level getTransitionState() {
		return transitionState;
	}
	public void setTransitionState(Level transitionState) {
		this.transitionState = transitionState;
	}

	public static enum Level {
		// ongoing (non-final)
		RUNNING,
		// waiting for external trigger, e.g. human interaction (non-final)
		WAITING,
		// a successful termination in between automation states, this should automatically be updated to either WAITING or RUNNING (non-final)
		STOPPED,
		// a runtime error occurred, this is a temporary state from which you can restart at some point or set to failed
		ERROR,
		// successfully concluded (a final state)
		SUCCEEDED,
		// the admin concluded that it will remain failed, this is a final state
		FAILED,
		// used for reverting on crash (non-final)
		REVERTED
	}
}
