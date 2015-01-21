package fr.inria.atlanmod.collaboro.backend;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import fr.inria.atlanmod.collaboro.history.HistoryFactory;
import fr.inria.atlanmod.collaboro.history.Proposal;
import fr.inria.atlanmod.collaboro.history.User;
import fr.inria.atlanmod.collaboro.history.Version;
import fr.inria.atlanmod.collaboro.metrics.Metric;
import fr.inria.atlanmod.collaboro.metrics.MetricResult;

public class CollaboroRecommenderBackend extends CollaboroBackend {
	private RecommenderEngine recommender;
	private String userId;
	private File metricsConfig;
	private List<Proposal> recommendations = new ArrayList<Proposal>();
	
	
	public CollaboroRecommenderBackend(ModelManager modelManager, String userId, File metricsConfig) {
		super(modelManager);
		this.userId = userId;
		this.metricsConfig = metricsConfig;
		checkRecommenderUser();
		this.recommender = new RecommenderEngine(userId, metricsConfig, this);
	}
	
	public void checkRecommenderUser() {
		boolean found = false;
		for(User user : getHistory().getUsers()) {
			if(user.getId().equals(this.userId)) {
				found = true;
				break;
			}
		}
		
		if(!found) {
			User recommenderUser = HistoryFactory.eINSTANCE.createUser();
			recommenderUser.setId(userId);
			recommenderUser.setFirstName("Recommender");
			recommenderUser.setLastName("System");
			recommenderUser.setEmail("recommender@collaboro.com");
			recommenderUser.setPassword("");
			
			getHistory().getUsers().add(recommenderUser);
			saveHistory();
		}
	}
	
	public boolean checkAlreadyRecommended() {
		Version version = getHistory().getHistories().get(getHistoryTracked()).getVersions().get(getVersionTracked());
		boolean alreadyRecommended = version.isRecommended();
		
		if(alreadyRecommended)
			this.recommendations = new ArrayList<Proposal>();
		
		return alreadyRecommended;
	}
	
	@Override
	public String createProposalPlain(String userId, String rationale, String referredElements) {
		Proposal newProposal = HistoryFactory.eINSTANCE.createProposal();
		initCollaborationPlain(newProposal, userId, rationale, referredElements);
		recommendations.add(newProposal);
		return newProposal.getId();
	}
	
	public void launchRecommender() {
		recommendations = new ArrayList<Proposal>();
		this.recommender = new RecommenderEngine(userId, metricsConfig, this);
		recommender.checkMetrics();
	}
	
	public void applyRecommendations() {
		for(Proposal recommendation : recommendations) {
			createProposal(recommendation);
		}

		Version version = getHistory().getHistories().get(getHistoryTracked()).getVersions().get(getVersionTracked());
		//version.setRecommended(true);
	}
	
	public HashMap<Metric, List<MetricResult>> getResults() {
		if(recommender == null) 
			throw new IllegalStateException("There is no recommender yet. Please, call launcheRecommender before");
		return recommender.getResults();
	}
	
	

	public HashMap<Metric, List<String>> getMetric2proposalId() {
		return recommender.getMetric2proposalId();
	}

	public List<Metric> getMetrics() {
		return recommender.getMetrics();
	}

	public void deactivateMetric(String metricId) {
		recommender.deactivateMetric(metricId);
	}

	public void activateMetric(String metricId) {
		recommender.activateMetric(metricId);
	}	
	
	

}