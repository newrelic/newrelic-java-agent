package com.newrelic.instrumentation.kotlin.coroutines_19;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;

/*
 *  Used to ignore Dispatched Tasks that the user has configured to ignore
 */
public class DispatchedTaskIgnores {

	private static final List<String> ignoredTasks = new ArrayList<>();
	private static final List<Pattern> ignoredTasksPatterns = new ArrayList<>();
	private static final String DISPATCHED_IGNORES_CONFIG = "Coroutines.ignores.dispatched";

	static {
		Config config = NewRelic.getAgent().getConfig();
		String ignores = config.getValue(DISPATCHED_IGNORES_CONFIG);
		configure(ignores);

	}

	public static void addIgnoredTasks(Collection<String> toIgnore, Collection<String> toIgnoreRegEx) {
		ignoredTasks.addAll(toIgnore);
		NewRelic.getAgent().getLogger().log(Level.FINER, "Ignoring dispatched tasks: {0}", ignoredTasks);
		for(String task : toIgnoreRegEx) {
			ignoredTasksPatterns.add(Pattern.compile(task));
		}
		NewRelic.getAgent().getLogger().log(Level.FINER, "Ignoring dispatched tasks matching regexs : {0}", ignoredTasksPatterns);
	}

	public static boolean ignoreDispatchedTask(String contString) {
		for(Pattern pattern : ignoredTasksPatterns) {
			if(pattern.matcher(contString).matches()) {
				return true;
			}
		}
		return ignoredTasks.contains(contString);
	}

	public static void addIgnoredReEx(String regEx) {
		if(!ignoredTasksPatterns.contains(Pattern.compile(regEx))) {
			ignoredTasksPatterns.add(Pattern.compile(regEx));
		}
	}

	public static void addIgnore(String ignore) {
		if(!ignoredTasks.contains(ignore)) {
			ignoredTasks.add(ignore);
			NewRelic.getAgent().getLogger().log(Level.FINE, "Will ignore DispatchedTasks with continuation string {0}", ignore);
		}
	}

	public static void reset() {
		ignoredTasks.clear();
	}

	protected static void configure(String result) {
		if(result == null || result.isEmpty()) return;
		String[] ignores = result.split(",");
		for(String ignore : ignores) {
			addIgnore(ignore);
		}
	}

	protected static void configureRegEx(String result) {
		if(result == null || result.isEmpty()) return;
		String[] ignores = result.split(",");
		for(String ignore : ignores) {
			addIgnoredReEx(ignore);
		}
	}
}
