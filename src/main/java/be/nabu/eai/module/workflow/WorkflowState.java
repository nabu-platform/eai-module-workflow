package be.nabu.eai.module.workflow;

import java.util.ArrayList;
import java.util.List;

public class WorkflowState {
	// a generated if for this state
	private String id;
	// the name of the state
	private String name, description;
	// the possible transitions out of this state
	private List<WorkflowTransition> transitions;
	// you can extend this state with other states
	// this allows for reusable transitions
	private List<String> extensions;
	// visual positioning
	private double x, y;
	// a global state is inherently extended by all other states, this means it has no extensions, nothing going to it but it is _not_ an initial state
	// nor will it require a force to run it, no matter in which state the workflow is
	// this is mostly interesting for generic stuff like cancel or for example adding a comment, attachment... something that is always allowed
	private boolean globalState;
	// final states are normally calculated
	// however, you can explicitly choose to mark a state as final (or not final), use with caution
	private Boolean finalState;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public List<WorkflowTransition> getTransitions() {
		if (transitions == null) {
			transitions = new ArrayList<WorkflowTransition>();
		}
		return transitions;
	}
	public void setTransitions(List<WorkflowTransition> transitions) {
		this.transitions = transitions;
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public List<String> getExtensions() {
		return extensions;
	}
	public void setExtensions(List<String> extensions) {
		this.extensions = extensions;
	}
	public boolean isGlobalState() {
		return globalState;
	}
	public void setGlobalState(boolean globalState) {
		this.globalState = globalState;
	}
	public Boolean getFinalState() {
		return finalState;
	}
	public void setFinalState(Boolean finalState) {
		this.finalState = finalState;
	}
}
