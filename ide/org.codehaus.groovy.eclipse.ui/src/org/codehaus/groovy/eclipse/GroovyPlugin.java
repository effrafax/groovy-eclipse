 /*
 * Copyright 2003-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.eclipse;
import java.io.IOException;

import org.codehaus.groovy.eclipse.core.GroovyCore;
import org.codehaus.groovy.eclipse.core.preferences.PreferenceConstants;
import org.codehaus.groovy.eclipse.debug.ui.EnsureJUnitFont;
import org.codehaus.groovy.eclipse.editor.GroovyTextTools;
import org.codehaus.groovy.eclipse.preferences.AskToConvertLegacyProjects;
import org.codehaus.groovy.eclipse.refactoring.actions.DelegatingCleanUpPostSaveListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.internal.Workbench;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public class GroovyPlugin extends AbstractUIPlugin {
	
    private final class JUnitPageListener implements IPageListener {
        public void pageOpened(IWorkbenchPage page) {
            IPartService service = (IPartService) page.getActivePart().getSite().getService(IPartService.class);
            service.addPartListener(ensure);
        }

        public void pageClosed(IWorkbenchPage page) {
            try {
                IPartService service = (IPartService) page.getWorkbenchWindow().getService(IPartService.class);
                service.removePartListener(ensure);
            } catch (Exception e) {
                GroovyCore.logException("Exception thrown when removing JUnit Monospace font listener", e);
            }
        }

        public void pageActivated(IWorkbenchPage page) {
        }
    }

    /**
	 * The single plugin instance
	 */
	private static GroovyPlugin plugin;
	
	static boolean trace;

	private GroovyTextTools textTools;

	public static final String PLUGIN_ID = "org.codehaus.groovy.eclipse.ui";

	public static final String GROOVY_TEMPLATE_CTX = "org.codehaus.groovy.eclipse.templates";
	
	private ContributionContextTypeRegistry fContextTypeRegistry;
	    
	private ContributionTemplateStore fTemplateStore;

    private EnsureJUnitFont ensure;

    private IPageListener junitListener;
	
	static {
		String value = Platform
				.getDebugOption("org.codehaus.groovy.eclipse/trace"); //$NON-NLS-1$
		if (value != null && value.equalsIgnoreCase("true")) //$NON-NLS-1$
			GroovyPlugin.trace = true;
	}

	/**
	 * The constructor.
	 */
	public GroovyPlugin() {
		super();
		plugin = this;
    }

	/**
	 * @return Returns the plugin instance.
	 */
	public static GroovyPlugin getDefault() {
		return plugin;
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow workBenchWindow= getActiveWorkbenchWindow();
		if (workBenchWindow == null) {
			return null;
		}
		Shell shell = workBenchWindow.getShell();
		if (shell == null) {
		    shell = plugin.getWorkbench().getDisplay().getActiveShell();
		}
		return shell;
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		if (plugin == null) {
			return null;
		}
		IWorkbench workBench= plugin.getWorkbench();
		if (workBench == null) {
		    return null;
		}
		return workBench.getActiveWorkbenchWindow();
	}
	
	/**
	 * Logs an exception
	 * 
	 * @param message The message to save.
	 * @param exception The exception to be logged.
	 */
	public void logException(String message, Exception exception) {
		log(IStatus.ERROR, message, exception);
	}
	
    /**
     * Logs a warning.
     * 
     * @param message The warning to log.
     */
    public void logWarning( final String message ){
    	log(IStatus.WARNING, message, null);
    }
    
	/**
	 * Logs an information message.
	 * 
	 * @param message The message to log.
	 */
	public void logTraceMessage(String message) {
		log(IStatus.INFO, message, null);
	}
	
	private void log(int severity, String message, Exception exception) {
		final IStatus status = new Status( severity, getBundle().getSymbolicName(), 0, message, exception );
        getLog().log( status );
	}

	/**
	 * @return Returns the dialogProvider.
	 */
//	public GroovyDialogProvider getDialogProvider() {
//		return dialogProvider;
//	}
//
//	/**
//	 * @param dialogProvider
//	 *            The dialogProvider to set.
//	 */
//	public void setDialogProvider(GroovyDialogProvider dialogProvider) {
//		this.dialogProvider = dialogProvider;
//	}

	public static void trace(String message) {
		if (trace) {
			getDefault().logTraceMessage("trace: " + message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		textTools = new GroovyTextTools();
		addMonospaceFontListener();
		DelegatingCleanUpPostSaveListener.installCleanUp();
	}

    private void addMonospaceFontListener() {
        ensure = new EnsureJUnitFont();
        junitListener = new JUnitPageListener();
        try {
            // listen for activations of the JUnit view and ensure monospace font if requested.
            Workbench.getInstance().getActiveWorkbenchWindow().addPageListener(junitListener);
        } catch (NullPointerException e) {
            // ignore, UI has not been initialized yet
        }
        
        getPreferenceStore().addPropertyChangeListener(ensure);
        PrefUtil.getInternalPreferenceStore().addPropertyChangeListener(ensure);
        
        maybeAskToConvertLegacyProjects();
    }
    
    private void removeMonospaceFontListener() {
        try {
            // listen for activations of the JUnit view and ensure monospace font if requested.
            if (! Workbench.getInstance().isClosing()) {
                Workbench.getInstance().getActiveWorkbenchWindow().removePageListener(junitListener);
            }
        } catch (NullPointerException e) {
            // ignore, UI has not been initialized yet
        } catch (SWTError e) {
            // workbench is shutting down.  can ignore this
        }
        getPreferenceStore().removePropertyChangeListener(ensure);
        PrefUtil.getInternalPreferenceStore().removePropertyChangeListener(ensure);
    }
	
	/**
     * 
     */
    private void maybeAskToConvertLegacyProjects() {
        AskToConvertLegacyProjects ask = new AskToConvertLegacyProjects();
        if (getPreferenceStore().getBoolean(PreferenceConstants.GROOVY_ASK_TO_CONVERT_LEGACY_PROJECTS)) {
            ask.schedule();
        }
    }

    @Override
	public void stop(BundleContext context) throws Exception {
	    super.stop(context);
	    textTools.dispose();
	    textTools = null;
        DelegatingCleanUpPostSaveListener.uninstallCleanUp();
        removeMonospaceFontListener();
	}

	
    public GroovyTextTools getTextTools() {
        return textTools;
    }
	

	public ContextTypeRegistry getContextTypeRegistry() {
		if (fContextTypeRegistry == null) {
			fContextTypeRegistry = new ContributionContextTypeRegistry();
			fContextTypeRegistry
				.addContextType(GROOVY_TEMPLATE_CTX);
		}
		return fContextTypeRegistry;
	}
	    
	public TemplateStore getTemplateStore() {
		if (fTemplateStore == null) {
			fTemplateStore = new ContributionTemplateStore(
					getContextTypeRegistry(),
					getDefault().getPreferenceStore(), "templates");
			try {
				fTemplateStore.load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return fTemplateStore;
	}
}