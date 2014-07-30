package fr.inria.atlanmod.collaboro.web.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

import fr.inria.atlanmod.collaboro.backend.CollaboroBackendFactory;
import fr.inria.atlanmod.collaboro.backend.CollaboroLanguageConfig;

/**
 * This class contains common features for all the servlets
 */
public class AbstractCollaboroServlet extends HttpServlet {
	private static final long serialVersionUID = 2L;

	Properties properties = null;

	@Override
	public void init() throws ServletException {
		super.init();
		properties = new Properties();
		try	{
			properties.load(getServletContext().getResourceAsStream("/WEB-INF/config.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(!CollaboroBackendFactory.isActive()) {
			String[] languages = properties.getProperty("languages").split(",");
			List<CollaboroLanguageConfig> languageConfigs = new ArrayList<>();
			for(String language : languages) {
				String languageName = properties.getProperty(language + ".name");
				String historyPath = properties.getProperty(language + ".history");
				String ecorePath = properties.getProperty(language + ".ecore");

				if(languageName != null && historyPath != null && ecorePath != null) {
					File historyFile = new File(historyPath);
					File ecoreFile = new File(ecorePath);
					if(historyFile.exists() && ecoreFile.exists()) {
						CollaboroLanguageConfig languageConfig = new CollaboroLanguageConfig(languageName, historyFile, ecoreFile);

						String versions = properties.getProperty(language + ".versions");
						if(versions != null) {
							String[] versionsWithExamples = versions.split(",");
							if(versionsWithExamples != null && versionsWithExamples.length > 0) {
								for(String versionWithExamples : versionsWithExamples) {
									String previousEcores = properties.getProperty(language + ".previous.version." + versionWithExamples + ".ecores");
									if(previousEcores != null) {
										int previousEcoresInt = Integer.valueOf(previousEcores);
										for(int i = 0; i < previousEcoresInt; i++) {
											String previousEcorePath = properties.getProperty(language + ".previous.version." + versionWithExamples + ".ecores." + String.valueOf(previousEcoresInt));
											if(previousEcorePath != null) {
												File previousEcoreFile = new File(previousEcorePath);
												if(previousEcoreFile.exists()) {
													languageConfig.addPreviousEcore(versionWithExamples, previousEcoreFile);
												}
											}
										}
									}
									String previousModels = properties.getProperty(language + ".previous.version." + versionWithExamples + ".models");
									if(previousModels != null) {
										int previousModelsInt = Integer.valueOf(previousModels);
										for(int i = 0; i < previousModelsInt; i++) {
											String previousModelPath = properties.getProperty(language + ".previous.version." + versionWithExamples + ".models." + String.valueOf(previousModelsInt));
											if(previousModelPath != null) {
												File previousModelFile = new File(previousModelPath);
												if(previousModelFile.exists()) {
													languageConfig.addPreviousModel(versionWithExamples, previousModelFile);
												}
											}
										}
									}
								}
							}
						}
						languageConfigs.add(languageConfig);
					}
				}
			}
			CollaboroBackendFactory.init(languageConfigs);
		}
	}

	/**
	 * Builds the response options to deal with CORS
	 * 
	 * @param response
	 */
	protected void addResponseOptions(HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "http://localhost:8001");
		response.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
		response.addHeader("Access-Control-Allow-Credentials", "true");
	}
}
