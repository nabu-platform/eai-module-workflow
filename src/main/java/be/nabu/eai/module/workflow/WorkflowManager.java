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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import be.nabu.eai.module.services.vm.VMServiceManager;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionMappingInterface;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionService;
import be.nabu.eai.module.workflow.transition.WorkflowTransitionServiceInterface;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.api.VariableRefactorArtifactManager;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.WrapperEntry;
import be.nabu.eai.repository.artifacts.container.ContainerArtifactManager.ContainerRepository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.vm.PipelineInterfaceProperty;
import be.nabu.libs.services.vm.SimpleVMServiceDefinition;
import be.nabu.libs.services.vm.api.VMService;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.Validation;

// Note: the workflow manager _must_ add references to its own generated children
// this is because of an odd reload bug on the server where the artifact needs to go through the reload cycle twice to reload properly: first by actually reloading the artifact iself
// then by reloading the children and their dependencies (which includes the artifact)
// note that for some reason on the first reload, the entry is marked as "not loaded" which is why it is bypassing the proper unloading/reloading of the children
public class WorkflowManager extends JAXBArtifactManager<WorkflowConfiguration, Workflow> implements ArtifactRepositoryManager<Workflow>, VariableRefactorArtifactManager<Workflow> {

	public WorkflowManager() {
		super(Workflow.class);
	}

	@Override
	protected Workflow newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new Workflow(id, container, repository);
	}
	
	
	public static UUID fromString(String uuid) {
		if (uuid == null) {
			return null;
		}
		if (uuid.indexOf('-') < 0) {
			uuid = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20);
		}
		return UUID.fromString(uuid);
	}
	

	@Override
	public List<String> getReferences(Workflow artifact) throws IOException {
		List<String> references = super.getReferences(artifact);
		for (DefinedStructure type : artifact.getStructures().values()) {
			references.addAll(StructureManager.getComplexReferences(type));
		}
		for (VMService service : artifact.getMappings().values()) {
			references.addAll(VMServiceManager.getReferencesForStep(service.getRoot()));
		}
		return references;
	}
	
	@Override
	public List<Validation<?>> updateReference(Workflow artifact, String from, String to) throws IOException {
		List<Validation<?>> results = new ArrayList<Validation<?>>();
		for (DefinedStructure type : artifact.getStructures().values()) {
			results.addAll(StructureManager.updateReferences(type, from, to));
		}
		for (VMService service : artifact.getMappings().values()) {
			VMServiceManager.updateReferences(service.getRoot(), from, to);
		}
		return results;
	}
	
	@Override
	public boolean updateVariableName(Workflow artifact, Artifact type, String oldPath, String newPath) {
		boolean updated = false;
		for (VMService service : artifact.getMappings().values()) {
			updated |= VMServiceManager.updateVariableName(service, oldPath, newPath);
		}
		return updated;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Workflow load(ResourceEntry entry, List<Validation<?>> messages) throws IOException, ParseException {
		Workflow workflow = super.load(entry, messages);
		ResourceContainer<?> privateDirectory = (ResourceContainer<?>) entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			ResourceContainer<?> structures = (ResourceContainer<?>) privateDirectory.getChild("structures");
			if (structures != null) {
				// structures inside a workflow can have random references to one another (through extension or actual reference or...)
				// to allow for this, we first load empty definitions into a temporary repository, this will allow us to set all the references correctly
				// afterwards we parse them one by one
				for (Resource child : (ResourceContainer<?>) structures) {
					DefinedStructure loaded = new DefinedStructure();
					String group;
					String name;
					if (child.getName().equals("properties")) {
						group = "types";
						name = "properties";
					}
					else {
						WorkflowState stateById = workflow.getStateById(fromString(child.getName()));
						if (stateById != null) {
							group = "types.states";
							name = EAIRepositoryUtils.stringToField(stateById.getName());
						}
						else {
							WorkflowTransition transitionById = workflow.getTransitionById(fromString(child.getName()));
							if (transitionById == null) {
								continue;
							}
							group = "types.transitions";
							name = EAIRepositoryUtils.stringToField(transitionById.getName());
						}
					}
					loaded.setId(entry.getId() + "." + group + "." + name);
					workflow.getStructures().put(child.getName(), loaded);
				}
				// we then create a temporary repository to allow referencing
				ContainerRepository containerRepository = new ContainerRepository(entry.getId(), (RepositoryEntry) entry, (Collection<Artifact>) (Collection) workflow.getStructures().values());
				containerRepository.setExactAliases(true);
				for (DefinedStructure structure : workflow.getStructures().values()) {
					containerRepository.alias(entry.getId() + ":" + structure.getId(), structure.getId());
				}
				// now we actually parse them!
				for (Resource child : (ResourceContainer<?>) structures) {
					new StructureManager().load(new WrapperEntry(containerRepository, entry, (ResourceContainer<?>) child, child.getName()), messages, workflow.getStructures().get(child.getName()));
				}
			}
			// Tricky stuff: the services that do the mapping are based on the documents we dynamically add to state
			ContainerRepository containerRepository = new ContainerRepository(entry.getId(), (RepositoryEntry) entry, (Collection<Artifact>) (Collection) workflow.getStructures().values());
			containerRepository.setExactAliases(true);
			for (DefinedStructure structure : workflow.getStructures().values()) {
				containerRepository.alias(entry.getId() + ":" + structure.getId(), structure.getId());
			}
			
			// we also generate ad-hoc interfaces for the services, this makes it a lot easier to update the interface (e.g. add new fields) later on
			if (workflow.getConfig().getStates() != null) {
				for (WorkflowState state : workflow.getConfig().getStates()) {
					if (state.getTransitions() != null) {
						for (WorkflowTransition transition : state.getTransitions()) {
//							WorkflowTransitionServiceInterface iface = new WorkflowTransitionServiceInterface(workflow, state, transition);
//							iface.setRepository(containerRepository);
//							containerRepository.addArtifacts(Arrays.asList(iface));
							WorkflowTransitionMappingInterface iface = new WorkflowTransitionMappingInterface(workflow, state, transition);
							containerRepository.addArtifacts(Arrays.asList(iface));
						}
					}
				}
			}
			
			// so we can only load them after the documents are added to the tree
			ResourceContainer<?> services = (ResourceContainer<?>) privateDirectory.getChild("services");
			if (services != null) {
				for (Resource child : (ResourceContainer<?>) services) {
					// make sure we set the pipeline interface property for consistent results
					WorkflowTransition transition = workflow.getTransitionById(fromString(child.getName()));
					WorkflowState fromState = workflow.getTransitionFromState(fromString(child.getName()));
					WorkflowTransitionMappingInterface iface = new WorkflowTransitionMappingInterface(workflow, fromState, transition);
					
					VMService loaded;
					try {
						WrapperEntry wrapperEntry = new WrapperEntry(containerRepository, entry, (ResourceContainer<?>) child, child.getName());
						loaded = new VMServiceManager().load(wrapperEntry, messages);
						((SimpleVMServiceDefinition) loaded).setId(entry.getId() + ".services." + (iface.isInitial() ? "initial" : "transition") + "." + EAIRepositoryUtils.stringToField(iface.getTransition().getName()));
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					
					loaded.getPipeline().setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), iface));
					
					workflow.getMappings().put(child.getName(), loaded);
				}
			}
		}
		
		return workflow;
	}

	@Override
	public List<Validation<?>> save(ResourceEntry entry, Workflow artifact) throws IOException {
		List<Validation<?>> messages = super.save(entry, artifact);
		if (messages == null) {
			messages = new ArrayList<Validation<?>>();
		}
		// remove all folders in private
		Resource privateDirectory = entry.getContainer().getChild(EAIResourceRepository.PRIVATE);
		if (privateDirectory != null) {
			((ManageableContainer<?>) entry.getContainer()).delete(EAIResourceRepository.PRIVATE);
		}
		privateDirectory = ((ManageableContainer<?>) entry.getContainer()).create(EAIResourceRepository.PRIVATE, Resource.CONTENT_TYPE_DIRECTORY);
		
		ManageableContainer<?> services = (ManageableContainer<?>) ((ManageableContainer<?>) privateDirectory).create("services", Resource.CONTENT_TYPE_DIRECTORY);
		ManageableContainer<?> structures = (ManageableContainer<?>) ((ManageableContainer<?>) privateDirectory).create("structures", Resource.CONTENT_TYPE_DIRECTORY);
		
		// save services
		for (String name : artifact.getMappings().keySet()) {
			// make sure we update the iface on save every time, if you have renamed or moved the workflow it might be out of sync
			VMService vmService = artifact.getMappings().get(name);
			
			DefinedServiceInterface iface = ValueUtils.getValue(PipelineInterfaceProperty.getInstance(), vmService.getPipeline().getProperties());
			// unset the interface property for saving
			vmService.getPipeline().setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), null));
			
			ManageableContainer<?> create = (ManageableContainer<?>) services.create(name, Resource.CONTENT_TYPE_DIRECTORY);
			messages.addAll(new VMServiceManager().save(new WrapperEntry(entry.getRepository(), entry, create, name), vmService));
			
			// reset the property after save
			vmService.getPipeline().setProperty(new ValueImpl<DefinedServiceInterface>(PipelineInterfaceProperty.getInstance(), iface));
		}
		
		// save structures
		for (String name : artifact.getStructures().keySet()) {
			ManageableContainer<?> create = (ManageableContainer<?>) structures.create(name, Resource.CONTENT_TYPE_DIRECTORY);
			messages.addAll(new StructureManager().save(new WrapperEntry(entry.getRepository(), entry, create, name), artifact.getStructures().get(name)));
		}
		return messages;
	}
	
	public void refreshChildren(ModifiableEntry parent, Workflow artifact) {
		removeChildren((ModifiableEntry) parent, artifact);
		addChildren((ModifiableEntry) parent, artifact);
	}

	@Override
	public List<Entry> addChildren(ModifiableEntry parent, Workflow artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		// add the state structures
		ModifiableEntry types = EAIRepositoryUtils.getParent(parent, "types", true);
		for (String id : artifact.getStructures().keySet()) {
			String group = null;
			String name;
			if (id.equals("properties")) {
				name = "properties";
			}
			else {
				WorkflowState stateById = artifact.getStateById(fromString(id));
				if (stateById != null) {
					name = EAIRepositoryUtils.stringToField(stateById.getName());
					group = "states";
				}
				else {
					WorkflowTransition transitionById = artifact.getTransitionById(fromString(id));
					if (transitionById != null) {
						name = EAIRepositoryUtils.stringToField(transitionById.getName());
						group = "transitions";
					}
					else {
						throw new RuntimeException("Could not find " + id);
					}
				}
			}
			ModifiableEntry directParent = group == null ? types : EAIRepositoryUtils.getParent(types, group, true);
			EAINode node = new EAINode();
			node.setArtifactClass(DefinedStructure.class);
			node.setArtifact(artifact.getStructures().get(id));
			node.setLeaf(true);
			Entry childEntry = new MemoryEntry(artifact.getId(), parent.getRepository(), directParent, node, directParent.getId() + "." + name, name);
			// need to explicitly set id (it was loaded from file)
			artifact.getStructures().get(id).setId(childEntry.getId());
			node.setEntry(childEntry);
			directParent.addChildren(childEntry);
			entries.add(childEntry);
		}
		
		// we need a service for each transition
		ModifiableEntry services = EAIRepositoryUtils.getParent(parent, "services", true);
		
		Map<UUID, WorkflowState> initialStates = new HashMap<UUID, WorkflowState>();
		List<UUID> targetedStates = new ArrayList<UUID>();
		for (WorkflowState state : artifact.getConfig().getStates()) {
			// extension states are not initial states, nor are global states
			if (!artifact.isExtensionState(state.getId()) && !state.isGlobalState()) {
				initialStates.put(state.getId(), state);
			}
			for (WorkflowTransition transition : state.getTransitions()) {
				targetedStates.add(transition.getTargetStateId());
			}
		}
		for (UUID targetedState : targetedStates) {
			initialStates.remove(targetedState);
		}
		
		// initial transitions that can create the workflow
		ModifiableEntry initial = EAIRepositoryUtils.getParent(services, "initial", true);
		for (WorkflowState state : initialStates.values()) {
			for (WorkflowTransition transition : state.getTransitions()) {
				EAINode node = new EAINode();
				node.setArtifactClass(DefinedService.class);
				WorkflowTransitionService service = new WorkflowTransitionService(new WorkflowTransitionServiceInterface(artifact, state, transition));
				node.setArtifact(service);
				node.setLeaf(true);
				Entry childEntry = new MemoryEntry(artifact.getId(), parent.getRepository(), initial, node, service.getId(), service.getName());
				if (!service.getName().equals(transition.getName())) {
					node.setName(transition.getName());
				}
				node.setEntry(childEntry);
				initial.addChildren(childEntry);
				entries.add(childEntry);
				transition.setOperationId(childEntry.getId());
			}
		}
		
		// other transitions that can be triggered on the workflow
		ModifiableEntry transitions = EAIRepositoryUtils.getParent(services, "transition", true);
		for (WorkflowState state : artifact.getConfig().getStates()) {
			if (!initialStates.containsValue(state)) {
				for (WorkflowTransition transition : state.getTransitions()) {
					EAINode node = new EAINode();
					node.setArtifactClass(DefinedService.class);
					WorkflowTransitionService service = new WorkflowTransitionService(new WorkflowTransitionServiceInterface(artifact, state, transition));
					node.setArtifact(service);
					node.setLeaf(true);
					Entry childEntry = new MemoryEntry(artifact.getId(), parent.getRepository(), transitions, node, service.getId(), service.getName());
					if (!service.getName().equals(transition.getName())) {
						node.setName(transition.getName());
					}
					node.setEntry(childEntry);
					transitions.addChildren(childEntry);
					entries.add(childEntry);	
					transition.setOperationId(childEntry.getId());
				}
			}
		}
		
		return entries;
	}

	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, Workflow artifact) {
		List<Entry> entries = new ArrayList<Entry>();
		ModifiableEntry structures = EAIRepositoryUtils.getParent(parent, "types", true);
		removeRecursively(structures, entries);
		ModifiableEntry services = EAIRepositoryUtils.getParent(parent, "services", true);
		removeRecursively(services, entries);
		entries.add(services);
		entries.add(structures);
		parent.removeChildren("services", "types");
		return entries;
	}
	
	public static void removeRecursively(ModifiableEntry parent, List<Entry> entries) {
		List<String> toRemove = new ArrayList<String>();
		for (Entry child : parent) {
			if (child instanceof ModifiableEntry) {
				removeRecursively((ModifiableEntry) child, entries);
			}
			entries.add(child);
			toRemove.add(child.getName());
		}
		parent.removeChildren(toRemove.toArray(new String[toRemove.size()]));
	}

}
