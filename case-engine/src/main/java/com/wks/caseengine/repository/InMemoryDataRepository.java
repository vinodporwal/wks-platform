package com.wks.caseengine.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.wks.caseengine.cases.definition.CaseDefinition;
import com.wks.caseengine.cases.definition.CaseStatus;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.form.Form;

public class InMemoryDataRepository implements DataRepository {

	private List<CaseInstance> caseInstancesRepo = new ArrayList<>();

	private List<CaseDefinition> caseDefinitionsRepo = new ArrayList<>();

	private List<Form> formsRepo = new ArrayList<>();

	@Override
	public List<CaseInstance> findCaseInstances(final Optional<CaseStatus> caseStatus) {
		Stream<CaseInstance> caseStream = caseInstancesRepo.stream();

		if (caseStatus.isPresent()) {
			caseInstancesRepo.stream().filter(o -> caseStatus.get().equals(o.getStatus()));
		}
		return caseStream.toList();
	}

	public void saveCaseInstance(final CaseInstance caseInstance) {
		caseInstancesRepo.add(caseInstance);
	}

	@Override
	// TODO test it
	public void updateCaseStatus(final String businessKey, final CaseStatus newStatus) throws Exception {
		List<CaseInstance> cases = caseInstancesRepo.stream().filter(o -> businessKey.equals(o.getBusinessKey()))
				.collect(Collectors.toList());
		cases.forEach(o -> o.setStatus(newStatus));
	}

	@Override
	public void deleteCaseInstance(final CaseInstance caseInstance) {
		caseInstancesRepo.remove(caseInstance);
	}

	@Override
	public CaseInstance getCaseInstance(final String businessKey) throws Exception {
		// TODO handle more than 1 result
		return caseInstancesRepo.stream().filter(o -> businessKey.equals(o.getBusinessKey())).findFirst().get();
	}

	@Override
	public List<CaseDefinition> findCaseDefintions() {
		return caseDefinitionsRepo;
	}

	@Override
	public CaseDefinition getCaseDefinition(final String caseDefId) {
		// TODO handle more than 1 result
		return caseDefinitionsRepo.stream().filter(o -> caseDefId.equals(o.getId())).findFirst().get();
	}

	@Override
	public void saveCaseDefinition(final CaseDefinition caseDefinition) {
		caseDefinitionsRepo.add(caseDefinition);
	}

	@Override
	public void deleteCaseDefinition(final String caseDefId) {
		caseDefinitionsRepo
				.remove(caseDefinitionsRepo.stream().filter(o -> caseDefId.equals(o.getId())).findFirst().get());
	}

	@Override
	public Form getForm(final String formKey) {
		// TODO handle more than 1 result
		return formsRepo.stream().filter(form -> formKey.equals(form.getKey())).findFirst().get();
	}

	@Override
	public void saveForm(final Form form) {
		formsRepo.add(form);
	}

	@Override
	public List<Form> findForms() {
		// TODO Auto-generated method stub
		return formsRepo;
	}

	@Override
	public void deleteForm(String formKey) throws Exception {
		formsRepo.remove(formsRepo.stream().filter(form -> formKey.equals(form.getKey())).findFirst().get());
	}

}
