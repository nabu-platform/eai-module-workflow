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

package be.nabu.eai.module.workflow;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Hidden;
import be.nabu.eai.api.LargeText;
import be.nabu.libs.services.vm.step.Invoke.KeyValueMapAdapter;
import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;

// can not directly refer to target state as this may result in circular references!!
// must refer to the id of the target state, separate resolving
@ComplexTypeDescriptor(propOrder =  {"id", "name", "targetStateId", "query", "queryOrder", "startBatch", "roles", "permissionContext", "permissionAction", "description", "x", "y", "line1FromX", "line1FromY", "line1ToX", "line1ToY",
		"line2FromX", "line2FromY", "line2ToX", "line2ToY", "allowMultipleAutomaticExecutions", "target", "targetProperties" })
public class WorkflowTransition implements Comparable<WorkflowTransition> {
	// a generated if for this state
	private UUID id;
	// the name of the state
	private String name, description;
	// the state we move to after the transition is done
	private UUID targetStateId;
	// the roles that are allowed to run this transition
	private List<String> roles;
	// query used to trigger this transition
	private String query;
	// the order in which the transition queries are executed, higher means later
	private int queryOrder;
	// whether or not this transition should start a batch
	private boolean startBatch;
	// the connecting circle
	private double x, y;
	// the following coordinates are mostly relevant when trying to redraw the diagram in other locations
	// the from and to coordinates of the line from state to circle
	private double line1FromX, line1FromY, line1ToX, line1ToY;
	// the from and to coordinates of the lines from circle to next state
	private double line2FromX, line2FromY, line2ToX, line2ToY;
	// the operation id for this transition
	private String operationId;
	// the permission context to use (if any)
	private String permissionContext, permissionAction;
	
	private String target;
	
	private Map<String, String> targetProperties;
	
	// @2021-04-16: this has no value by default because (for backwards compatibility) this is "false" for self transitions and "true" for non-self transitions by default
	private Boolean allowMultipleAutomaticExecutions;
	
	@XmlJavaTypeAdapter(value = UuidXmlAdapter.class)
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	@LargeText
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@XmlJavaTypeAdapter(value = UuidXmlAdapter.class)
	public UUID getTargetStateId() {
		return targetStateId;
	}
	public void setTargetStateId(UUID targetStateId) {
		this.targetStateId = targetStateId;
	}
	
	public List<String> getRoles() {
		return roles;
	}
	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
	@Field(comment = "If you set a query, it will be evaluated every time the workflow reaches this state. If the query result to 'true', the transition is automatically executed.")
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
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
	@Field(comment = "The numeric order in which the transition is executed relative to sibling transitions. For every order value, there can only be one transition that moves the workflow to another state.", show = "query != null && query != ''")
	public int getQueryOrder() {
		return queryOrder;
	}
	public void setQueryOrder(int queryOrder) {
		this.queryOrder = queryOrder;
	}
	@Override
	public int compareTo(WorkflowTransition o) {
		if (queryOrder == o.queryOrder) {
			// do alphabetic if order is the same
			return name.compareToIgnoreCase(o.getName());
		}
		return queryOrder - o.queryOrder;
	}
	@Field(comment = "When you configure a batch on a transition, a new batch id will be generated. You can start multiple new workflows linked to this batch id, the transition will only complete if all batch workflows are finished.")
	public boolean isStartBatch() {
		return startBatch;
	}
	public void setStartBatch(boolean startBatch) {
		this.startBatch = startBatch;
	}

	// should not be persisted but...in a hurry! @hackathon gsk 2018-01-18
	@Hidden
	@XmlTransient
	public String getOperationId() {
		return operationId;
	}
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getTargetProperties() {
		return targetProperties;
	}
	public void setTargetProperties(Map<String, String> targetProperties) {
		this.targetProperties = targetProperties;
	}
	
	public String getPermissionContext() {
		return permissionContext;
	}
	public void setPermissionContext(String permissionContext) {
		this.permissionContext = permissionContext;
	}
	public String getPermissionAction() {
		return permissionAction;
	}
	public void setPermissionAction(String permissionAction) {
		this.permissionAction = permissionAction;
	}
	public double getLine1FromX() {
		return line1FromX;
	}
	public void setLine1FromX(double line1FromX) {
		this.line1FromX = line1FromX;
	}
	public double getLine1FromY() {
		return line1FromY;
	}
	public void setLine1FromY(double line1FromY) {
		this.line1FromY = line1FromY;
	}
	public double getLine1ToX() {
		return line1ToX;
	}
	public void setLine1ToX(double line1ToX) {
		this.line1ToX = line1ToX;
	}
	public double getLine1ToY() {
		return line1ToY;
	}
	public void setLine1ToY(double line1ToY) {
		this.line1ToY = line1ToY;
	}
	public double getLine2FromX() {
		return line2FromX;
	}
	public void setLine2FromX(double line2FromX) {
		this.line2FromX = line2FromX;
	}
	public double getLine2FromY() {
		return line2FromY;
	}
	public void setLine2FromY(double line2FromY) {
		this.line2FromY = line2FromY;
	}
	public double getLine2ToX() {
		return line2ToX;
	}
	public void setLine2ToX(double line2ToX) {
		this.line2ToX = line2ToX;
	}
	public double getLine2ToY() {
		return line2ToY;
	}
	public void setLine2ToY(double line2ToY) {
		this.line2ToY = line2ToY;
	}
	public Boolean getAllowMultipleAutomaticExecutions() {
		return allowMultipleAutomaticExecutions;
	}
	@Field(comment = "Self transitions will only automatically trigger once by default, regular transitions can be triggered multiple times. Toggle this boolean to modify that behavior", show = "query != null && query != ''")
	public void setAllowMultipleAutomaticExecutions(Boolean allowMultipleAutomaticExecutions) {
		this.allowMultipleAutomaticExecutions = allowMultipleAutomaticExecutions;
	}
	
}
