package org.jenkinsci.plugins.archived_artifact_url_viewer;

import hudson.Extension;
import hudson.model.RootAction;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

@Extension
public class ArchivedArtifactUrlAction implements RootAction {

	public String getDisplayName() {
		return null;
	}

	public String getIconFileName() {
		return null;
	}

	public String getUrlName() {
		return "archivedArtifacts";
	}

	public void doArtifact(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
		String requestPath = req.getRestOfPath();
		if(requestPath.startsWith("/")) {
			requestPath = requestPath.substring(1);
		}

		String[] requestPathFragments = requestPath.split("/");

		String jobName = null;
		int buildNumber = -1;
		String archivePath = null;
		String requestedFilePath = null;
		if(requestPathFragments.length > 4) {
			jobName = requestPathFragments[0];

			try {
				buildNumber = Integer.parseInt(requestPathFragments[1]);
			}
			catch(NumberFormatException nFE) {

			}

			StringBuilder archivedArtifactPathBuilder = new StringBuilder();
			int archivedFileTokenIndex = -1;
			for(int i = 2; i < requestPathFragments.length; i++) {
				archivedArtifactPathBuilder.append(requestPathFragments[i]);
				if(requestPathFragments[i].endsWith(".zip") || requestPathFragments[i].endsWith(".jar")) {
					archivePath = archivedArtifactPathBuilder.toString();
					archivedFileTokenIndex = i + 1;
					break;
				}
				archivedArtifactPathBuilder.append("/");
			}

			if(archivedFileTokenIndex > 0) {
				StringBuilder requestedFilePathBuilder = new StringBuilder();
				for(int i = archivedFileTokenIndex; i < requestPathFragments.length; i++) {
					requestedFilePathBuilder.append(requestPathFragments[i]);
					if(i < requestPathFragments.length - 1) {
						requestedFilePathBuilder.append("/");
					}
				}
				requestedFilePath = requestedFilePathBuilder.toString();
			}
		}

		if(jobName != null && buildNumber >= 0 && archivePath != null && requestedFilePath != null) {
			AbstractProject<?, ?> project = (AbstractProject<?, ?>)Jenkins.getInstance().getItem(jobName);
			if(project != null) {
				AbstractBuild<?, ?> build = project.getBuildByNumber(buildNumber);
				if(build != null) {
					File artifactsDir = build.getArtifactsDir();
					File absoluteArchivePath = new File(artifactsDir, archivePath);
					if(absoluteArchivePath.exists() && absoluteArchivePath.isFile()) {
						ZipFile zipFile = new ZipFile(absoluteArchivePath);
						try {
							ZipEntry zipEntry = zipFile.getEntry(requestedFilePath);
							if(zipEntry != null) {
								rsp.serveFile(req, zipFile.getInputStream(zipEntry), zipEntry.getTime(), zipEntry.getSize(), zipEntry.getName());
							}
						}
						finally {
							zipFile.close();
						}
					}
				}
			}
		}
	}
}
