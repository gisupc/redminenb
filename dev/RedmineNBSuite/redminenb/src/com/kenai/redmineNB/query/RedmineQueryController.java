/*
 * Copyright 2012 Anchialas.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kenai.redmineNB.query;

import com.kenai.redmineNB.Redmine;
import com.kenai.redmineNB.RedmineConfig;
import com.kenai.redmineNB.RedmineConnector;
import com.kenai.redmineNB.RedmineException;
import com.kenai.redmineNB.issue.RedmineIssue;
import com.kenai.redmineNB.query.RedmineQueryParameter.CheckBoxParameter;
import com.kenai.redmineNB.query.RedmineQueryParameter.ListParameter;
import com.kenai.redmineNB.query.RedmineQueryParameter.TextFieldParameter;
import com.kenai.redmineNB.repository.RedmineRepository;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.logging.Level;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.apache.commons.lang.StringUtils;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.bugtracking.issuetable.Filter;
import org.netbeans.modules.bugtracking.issuetable.IssueTable;
import org.netbeans.modules.bugtracking.spi.BugtrackingController;
import org.netbeans.modules.bugtracking.spi.Issue;
import org.netbeans.modules.bugtracking.spi.Query;
import org.netbeans.modules.bugtracking.spi.QueryNotifyListener;
import org.netbeans.modules.bugtracking.ui.query.QueryAction;
import org.netbeans.modules.bugtracking.util.BugtrackingUtil;
import org.netbeans.modules.bugtracking.util.SaveQueryPanel;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.HtmlBrowser;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.redmine.ta.beans.IssueCategory;
import org.redmine.ta.beans.IssueStatus;
import org.redmine.ta.beans.Tracker;
import org.redmine.ta.beans.Version;


/**
 *
 * @author Anchialas <anchialas@gmail.com>
 */
@NbBundle.Messages({
   "MSG_SameName=Query with the same name already exists.",
   "MSG_NoResults=No Issues found",
   "MSG_NotFound=Issue #{0} not found",
   "MSG_Searching=Searching...",
   "MSG_SearchingQuery=Searching {0}...",
   "MSG_Opening=Opening Issue {0}...",
   "MSG_Populating=Reading server data from Issue Tracker ''{0}''...",
   "MSG_RemoveQuery=Do you want to remove the query ''{0}''?",
   "CTL_RemoveQuery=Remove",
   "LBL_RetrievingIssue=Retrieved issue {0}",
   "LBL_Never=Never",
   "LBL_MatchingIssues=There {0,choice,0#are no issues|1#is one issue|1<are {0,number,integer} issues} matching this query.",
   "LBL_SelectKeywords=Select or deselect keywords."
})
public class RedmineQueryController extends BugtrackingController implements DocumentListener, ItemListener, ListSelectionListener, ActionListener, KeyListener {

   final RedmineQueryPanel queryPanel;
   private final IssueTable issueTable;
   //
   private final RequestProcessor rp = new RequestProcessor("Redmine query", 1, true);  // NOI18N
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // NOI18N
   private final RedmineRepository repository;
   //
   private RedmineQuery query;
   //
   private final ListParameter versionParameter;
   private final ListParameter trackerParameter;
   private final ListParameter statusParameter;
   private final ListParameter categoryParameter;
   private final ListParameter priorityParameter;
   //private final ListParameter resolutionParameter;
   //private final ListParameter severityParameter;
   private final Map<String, RedmineQueryParameter> parameters;
   //
   private QueryTask refreshTask;


   public RedmineQueryController(RedmineRepository repository, RedmineQuery query) {
      this.repository = repository;
      this.query = query;

      issueTable = new IssueTable(query, query.getColumnDescriptors());
      //issueTable.setRenderer(...);
      queryPanel = new RedmineQueryPanel(issueTable.getComponent(), this);

      // set parameters
      parameters = new LinkedHashMap<String, RedmineQueryParameter>();

      versionParameter = createQueryParameter(ListParameter.class, queryPanel.versionList, "fixed_version_id");
      statusParameter = createQueryParameter(ListParameter.class, queryPanel.statusList, "status_id");
      trackerParameter = createQueryParameter(ListParameter.class, queryPanel.trackerList, "tracker_id");
      categoryParameter = createQueryParameter(ListParameter.class, queryPanel.categoryList, "category_id");
      priorityParameter = createQueryParameter(ListParameter.class, queryPanel.priorityList, "priority_id");

      createQueryParameter(TextFieldParameter.class, queryPanel.queryTextField, "query");
      createQueryParameter(CheckBoxParameter.class, queryPanel.qSubjectCheckBox, "is_subject");
      createQueryParameter(CheckBoxParameter.class, queryPanel.qDescriptionCheckBox, "is_description");
      createQueryParameter(CheckBoxParameter.class, queryPanel.qCommentsCheckBox, "is_comments");


      setListeners();
      postPopulate();
   }


   private void setListeners() {
      queryPanel.filterComboBox.addItemListener(this);
      queryPanel.searchButton.addActionListener(this);
      queryPanel.refreshCheckBox.addActionListener(this);
      queryPanel.saveChangesButton.addActionListener(this);
      queryPanel.cancelChangesButton.addActionListener(this);
      queryPanel.gotoIssueButton.addActionListener(this);
      queryPanel.webButton.addActionListener(this);
      queryPanel.saveButton.addActionListener(this);
      queryPanel.refreshButton.addActionListener(this);
      queryPanel.modifyButton.addActionListener(this);
      queryPanel.seenButton.addActionListener(this);
      queryPanel.removeButton.addActionListener(this);
      queryPanel.refreshConfigurationButton.addActionListener(this);
      queryPanel.findIssuesButton.addActionListener(this);
      queryPanel.cloneQueryButton.addActionListener(this);

      queryPanel.issueIdTextField.addActionListener(this);
      queryPanel.categoryList.addKeyListener(this);
      queryPanel.versionList.addKeyListener(this);
      queryPanel.statusList.addKeyListener(this);
      queryPanel.resolutionList.addKeyListener(this);
      queryPanel.severityList.addKeyListener(this);
      queryPanel.priorityList.addKeyListener(this);

      queryPanel.queryTextField.addActionListener(this);
      queryPanel.peopleTextField.addActionListener(this);
   }


   @Override
   public JComponent getComponent() {
      return queryPanel;
   }


   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }


   @Override
   public boolean isValid() {
      return true;
   }


   @Override
   public void applyChanges() throws IOException {
      System.out.println("applyChanges()");
   }

   // Listener implementations /////////////////////////////////////////////////

   @Override
   public void insertUpdate(DocumentEvent e) {
      fireDataChanged();
   }


   @Override
   public void removeUpdate(DocumentEvent e) {
      fireDataChanged();
   }


   @Override
   public void changedUpdate(DocumentEvent e) {
      fireDataChanged();
   }


   @Override
   public void itemStateChanged(ItemEvent e) {
      fireDataChanged();
      if (e.getSource() == queryPanel.filterComboBox) {
         onFilterChange((Filter) e.getItem());
      }
   }


   @Override
   public void valueChanged(ListSelectionEvent e) {
      fireDataChanged();            // XXX do we need this ???
   }


   @Override
   public void actionPerformed(ActionEvent e) {
      try {
         if (e.getSource() == queryPanel.searchButton) {
            onRefresh();
         } else if (e.getSource() == queryPanel.gotoIssueButton) {
            onGotoIssue();
         } else if (e.getSource() == queryPanel.saveChangesButton) {
            onSave(true); // refresh
         } else if (e.getSource() == queryPanel.cancelChangesButton) {
            onCancelChanges();
         } else if (e.getSource() == queryPanel.webButton) {
            onWeb();
         } else if (e.getSource() == queryPanel.saveButton) {
            onSave(false); // do not refresh
         } else if (e.getSource() == queryPanel.refreshButton) {
            onRefresh();
         } else if (e.getSource() == queryPanel.modifyButton) {
            onModify();
         } else if (e.getSource() == queryPanel.seenButton) {
            onMarkSeen();
         } else if (e.getSource() == queryPanel.removeButton) {
            onRemove();
         } else if (e.getSource() == queryPanel.refreshCheckBox) {
            onAutoRefresh();
         } else if (e.getSource() == queryPanel.refreshConfigurationButton) {
            onRefreshConfiguration();
         } else if (e.getSource() == queryPanel.findIssuesButton) {
            onFindIssues();
         } else if (e.getSource() == queryPanel.cloneQueryButton) {
            onCloneQuery();
         } else if (e.getSource() == queryPanel.issueIdTextField) {
            if (!queryPanel.issueIdTextField.getText().trim().equals("")) {                // NOI18N
               onGotoIssue();
            }
         } else if (e.getSource() == queryPanel.issueIdTextField
                 || e.getSource() == queryPanel.queryTextField
                 || e.getSource() == queryPanel.peopleTextField) {
            onRefresh();
         }
      } catch (RedmineException ex) {
         Exceptions.printStackTrace(ex);
      }
   }


   @Override
   public void keyTyped(KeyEvent e) {
      // do nothing
   }


   @Override
   public void keyPressed(KeyEvent e) {
      // do nothing
   }


   @Override
   public void keyReleased(KeyEvent e) {
      if (e.getKeyCode() != KeyEvent.VK_ENTER) {
         return;
      }
      if (e.getSource() == queryPanel.categoryList
              || e.getSource() == queryPanel.versionList
              || e.getSource() == queryPanel.statusList
              || e.getSource() == queryPanel.resolutionList
              || e.getSource() == queryPanel.priorityList) {
         onRefresh();
      }
   }

   /////////////////////////////////////////////////////////////////////////////

   private void onFilterChange(Filter filter) {
      selectFilter(filter);
   }


   private void onSave(final boolean refresh) throws RedmineException {
      Redmine.getInstance().getRequestProcessor().post(new Runnable() {

         @Override
         public void run() {
            Redmine.LOG.fine("on save start");
            String name = query.getDisplayName();
            if (!query.isSaved()) {
               name = getSaveName();
               if (name == null) {
                  return;
               }
            }
            assert name != null;
            save(name);
            Redmine.LOG.fine("on save finnish");

            if (refresh) {
               onRefresh();
            }
         }

      });
   }


   /**
    * Saves the query under the given name
    *
    * @param name
    */
   private void save(String name) {
      Redmine.LOG.log(Level.FINE, "saving query '{0}'", new Object[]{name});
      query.setName(name);
      repository.saveQuery(query);
      query.setSaved(true); // XXX
      setAsSaved();
      if (!query.wasRun()) {
         Redmine.LOG.log(Level.FINE, "refreshing query '{0}' after save", new Object[]{name});
         onRefresh();
      }
      Redmine.LOG.log(Level.FINE, "query '{0}' saved", new Object[]{name});
   }


   private String getSaveName() {
      SaveQueryPanel.QueryNameValidator v = new SaveQueryPanel.QueryNameValidator() {

         @Override
         public String isValid(String name) {
            Query[] queries = repository.getQueries();
            for (Query q : queries) {
               if (q.getDisplayName().equals(name)) {
                  return Bundle.MSG_SameName();
               }
            }
            return null;
         }

      };
      return SaveQueryPanel.show(v, new HelpCtx("com.kenai.redmineNB.query.savePanel"));
   }


   private void onCancelChanges() {
//        if(query.getDisplayName() != null) { // XXX need a better semantic - isSaved?
//            String urlParameters = RedmineConfig.getInstance().getUrlParams(repository, query.getDisplayName());
//            if(urlParameters != null) {
//                setParameters(urlParameters);
//            }
//        }
      setAsSaved();
   }


   public void selectFilter(final Filter filter) {
      if (filter != null) {
         // XXX this part should be handled in the issues table - move the filtercombo and the label over
         Issue[] issues = query.getIssues();
         int c = 0;
         if (issues != null) {
            for (Issue issue : issues) {
               if (filter.accept(issue)) {
                  c++;
               }
            }
         }
         final int issueCount = c;

         Runnable r = new Runnable() {

            @Override
            public void run() {
               queryPanel.filterComboBox.setSelectedItem(filter);
               setIssueCount(issueCount);
            }

         };
         if (EventQueue.isDispatchThread()) {
            r.run();
         } else {
            EventQueue.invokeLater(r);
         }
      }
      issueTable.setFilter(filter);
   }


   private void setAsSaved() {
      queryPanel.setSaved(query.getDisplayName(), getLastRefresh());
      queryPanel.setModifyVisible(false);
      queryPanel.refreshCheckBox.setVisible(true);
   }


   private String getLastRefresh() throws MissingResourceException {
      long l = query.getLastRefresh();
      return l > 0
              ? dateFormat.format(new Date(l))
              : Bundle.LBL_Never();
   }


   private void onGotoIssue() throws RedmineException {
      final Long issueId = (Long) queryPanel.issueIdTextField.getValue();
      if (issueId == null) {
         return;
      }

      final RequestProcessor.Task[] t = new RequestProcessor.Task[1];
      Cancellable c = new Cancellable() {

         @Override
         public boolean cancel() {
            if (t[0] != null) {
               return t[0].cancel();
            }
            return true;
         }

      };
      final ProgressHandle handle = ProgressHandleFactory.createHandle(Bundle.MSG_Opening(issueId), c); // NOI18N
      t[0] = Redmine.getInstance().getRequestProcessor().create(new Runnable() {

         @Override
         public void run() {
            handle.start();
            try {
               RedmineIssue issue = (RedmineIssue) repository.getIssue(String.valueOf(issueId));
               if (issue == null) {
                  queryPanel.setGoToIssueInfo("exclamation.png", Bundle.MSG_NotFound(issueId));
               } else {
                  queryPanel.setGoToIssueInfo(null, null);
                  issue.open();
               }
            } finally {
               handle.finish();
            }
         }

      });
      t[0].schedule(0);
   }



   private void onWeb() throws RedmineException {
      String params = ""; //query.getUrlParameters();
      String repoURL = repository.getUrl();
      final String urlString = repoURL + (params != null && !params.equals("") ? params : ""); // NOI18N

      Redmine.getInstance().getRequestProcessor().post(new Runnable() {

         @Override
         public void run() {
            URL url;
            try {
               url = new URL(urlString);
            } catch (MalformedURLException ex) {
               Redmine.LOG.log(Level.SEVERE, null, ex);
               return;
            }
            HtmlBrowser.URLDisplayer displayer = HtmlBrowser.URLDisplayer.getDefault();
            if (displayer != null) {
               displayer.showURL(url);
            } else {
               // XXX nice error message?
               Redmine.LOG.warning("No URLDisplayer found.");             // NOI18N
            }
         }

      });
   }


   private void onCloneQuery() {
      RedmineQuery q = new RedmineQuery(null, repository, false, false, true);
      BugtrackingUtil.openQuery(q, repository, false);
   }


   public void autoRefresh() {
      onRefresh(true);
   }


   public void onRefresh() {
      onRefresh(false);
   }


   private void onRefresh(final boolean auto) {
      if (refreshTask == null) {
         refreshTask = new QueryTask();
      } else {
         refreshTask.cancel();
      }
      refreshTask.post(auto);
   }


   private void onModify() {
      queryPanel.setModifyVisible(true);
   }


   private void onMarkSeen() throws RedmineException {
      Redmine.getInstance().getRequestProcessor().post(new Runnable() {

         @Override
         public void run() {
            Issue[] issues = query.getIssues();
            for (Issue issue : issues) {
               try {
                  ((RedmineIssue) issue).setSeen(true);
               } catch (IOException ex) {
                  Redmine.LOG.log(Level.SEVERE, null, ex);
               }
            }
         }

      });
   }


   private void onRemove() throws RedmineException {
      NotifyDescriptor nd = new NotifyDescriptor.Confirmation(Bundle.MSG_RemoveQuery(query.getDisplayName()),
                                                              Bundle.CTL_RemoveQuery(),
                                                              NotifyDescriptor.OK_CANCEL_OPTION);
      if (DialogDisplayer.getDefault().notify(nd) == NotifyDescriptor.OK_OPTION) {
         Redmine.getInstance().getRequestProcessor().post(new Runnable() {

            @Override
            public void run() {
               remove();
            }

         });
      }
   }


   private void onFindIssues() {
      //Query.openNew(repository);
      QueryAction.openQuery(query, repository);
   }


   private void onAutoRefresh() {
      final boolean autoRefresh = queryPanel.refreshCheckBox.isSelected();
      RedmineConfig.getInstance().setQueryAutoRefresh(query.getDisplayName(), autoRefresh);
      logAutoRefreshEvent(autoRefresh);
      if (autoRefresh) {
         scheduleForRefresh();
      } else {
         repository.stopRefreshing(query);
      }
   }


   protected void scheduleForRefresh() {
      if (query.isSaved()) {
         repository.scheduleForRefresh(query);
      }
   }


   protected void logAutoRefreshEvent(boolean autoRefresh) {
      BugtrackingUtil.logAutoRefreshEvent(
              RedmineConnector.getConnectorName(),
              query.getDisplayName(),
              false,
              autoRefresh);
   }


   private void onRefreshConfiguration() {
//      postPopulate(query.getUrlParameters(), true);
      postPopulate();
   }


   protected final void postPopulate() {

      final RequestProcessor.Task[] t = new RequestProcessor.Task[1];
      Cancellable c = new Cancellable() {

         @Override
         public boolean cancel() {
            if (t[0] != null) {
               return t[0].cancel();
            }
            return true;
         }

      };

      final String msgPopulating = Bundle.MSG_Populating(repository.getDisplayName());
      final ProgressHandle handle = ProgressHandleFactory.createHandle(msgPopulating, c);

      EventQueue.invokeLater(new Runnable() {

         @Override
         public void run() {
            enableFields(false);
            queryPanel.showRetrievingProgress(true, msgPopulating, !query.isSaved());
            handle.start();
         }

      });

      t[0] = rp.post(new Runnable() {

         @Override
         public void run() {
            try {
               populate();

            } finally {
               EventQueue.invokeLater(new Runnable() {

                  @Override
                  public void run() {
                     enableFields(true);
                     handle.finish();
                     queryPanel.showRetrievingProgress(false, null, !query.isSaved());
                  }

               });
            }
         }

      });
   }


   protected void populate() {
      if (Redmine.LOG.isLoggable(Level.FINE)) {
         Redmine.LOG.log(Level.FINE, "Starting populate query controller {0}", (query.isSaved() ? " - " + query.getDisplayName() : "")); // NOI18N
      }
      try {
         EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
               populateProjectDetails();

               if (query.isSaved()) {
                  boolean autoRefresh = RedmineConfig.getInstance().getQueryAutoRefresh(query.getDisplayName());
                  queryPanel.refreshCheckBox.setSelected(autoRefresh);
               }
            }

         });
      } finally {
         if (Redmine.LOG.isLoggable(Level.FINE)) {
            Redmine.LOG.log(Level.FINE, "Finnished populate query controller{0}", (query.isSaved() ? " - " + query.getDisplayName() : "")); // NOI18N
         }
      }
   }


   private void populateProjectDetails() {
      // Versions
      List<ParameterValue> pvList = new ArrayList<ParameterValue>();
      for (Version v : repository.getVersions()) {
         pvList.add(new ParameterValue(v.getName(), v.getId()));
      }
      versionParameter.setParameterValues(pvList);

      // Tracker
      pvList = new ArrayList<ParameterValue>();
      for (Tracker t : repository.getTrackers()) {
         pvList.add(new ParameterValue(t.getName(), t.getId()));
      }
      trackerParameter.setParameterValues(pvList);

      // Issue Status
      pvList = new ArrayList<ParameterValue>();
      pvList.add(new ParameterValue("open"));
      pvList.add(new ParameterValue("closed"));
      pvList.add(null);
      for (IssueStatus s : repository.getStatuses()) {
         pvList.add(new ParameterValue(s.getName(), s.getId()));
      }
      statusParameter.setParameterValues(pvList);

      // Issue Category
      pvList = new ArrayList<ParameterValue>();
      for (IssueCategory c : repository.getIssueCategories()) {
         pvList.add(new ParameterValue(c.getName(), c.getId()));
      }
      categoryParameter.setParameterValues(pvList);


      // Issue Priorities
      pvList = repository.getIssuePriorities();
      priorityParameter.setParameterValues(pvList);

   }


   private <T extends RedmineQueryParameter> T createQueryParameter(Class<T> clazz, Component c, String parameter) {
      try {
         Constructor<T> constructor = clazz.getConstructor(c.getClass(), String.class);
         T t = constructor.newInstance(c, parameter);
         parameters.put(parameter, t);
         return t;
      } catch (Exception ex) {
         Redmine.LOG.log(Level.SEVERE, parameter, ex);
      }
      return null;
   }


   public Map<String, String> getSearchParameterMap() {
      Map<String, String> m = new HashMap<String, String>();
      m.put("project_id", String.valueOf(repository.getProject().getId()));
      for (RedmineQueryParameter p : parameters.values()) {
         String str = p.getValueString();
         if (StringUtils.isNotBlank(str)) {
            m.put(p.getParameter(), str);
         }
      }
      return m;
   }


   protected void enableFields(boolean bl) {
      // set all non parameter fields
      queryPanel.enableFields(bl);
      // set the parameter fields
      for (Map.Entry<String, RedmineQueryParameter> e : parameters.entrySet()) {
         RedmineQueryParameter pv = parameters.get(e.getKey());
         pv.setEnabled(bl && !pv.isEmpty());
      }
   }


   private void remove() {
      if (refreshTask != null) {
         refreshTask.cancel();
      }
      query.remove();
   }


   private void setIssueCount(final int count) {
      EventQueue.invokeLater(new Runnable() {

         @Override
         public void run() {
            queryPanel.tableSummaryLabel.setText(Bundle.LBL_MatchingIssues(count));
         }

      });
   }


   void switchToDeterminateProgress(long issuesCount) {
      if (refreshTask != null) {
         refreshTask.switchToDeterminateProgress(issuesCount);
      }
   }


   void addProgressUnit(String issueDesc) {
      if (refreshTask != null) {
         refreshTask.addProgressUnit(issueDesc);
      }
   }


   private class QueryTask implements Runnable, Cancellable, QueryNotifyListener {

      private ProgressHandle handle;
      private RequestProcessor.Task task;
      private int counter;
      private boolean autoRefresh;
      private long progressMaxWorkunits;
      private int progressWorkunits;


      public QueryTask() {
         query.addNotifyListener(this);
      }


      private synchronized void startQuery() {
         handle = ProgressHandleFactory.createHandle(
                 Bundle.MSG_SearchingQuery(query.getDisplayName() != null
                 ? query.getDisplayName()
                 : repository.getDisplayName()),
                 this);
         EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
               enableFields(false);
               queryPanel.showSearchingProgress(true, Bundle.MSG_Searching());
            }

         });
         handle.start();
      }


      private synchronized void finnishQuery() {
         task = null;
         if (handle != null) {
            handle.finish();
            handle = null;
         }
         EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
               queryPanel.setQueryRunning(false);
               queryPanel.setLastRefresh(getLastRefresh());
               queryPanel.showNoContentPanel(false);
               enableFields(true);
            }

         });
      }


      synchronized void switchToDeterminateProgress(long progressMaxWorkunits) {
         if (handle != null) {
            handle.switchToDeterminate((int) progressMaxWorkunits);
            this.progressMaxWorkunits = progressMaxWorkunits;
            this.progressWorkunits = 0;
         }
      }


      synchronized void addProgressUnit(String issueDesc) {
         if (handle != null && progressWorkunits < progressMaxWorkunits) {
            handle.progress(Bundle.LBL_RetrievingIssue(issueDesc), ++progressWorkunits);
         }
      }


      public void executeQuery() {
         setQueryRunning(true);
         try {
            query.refresh(autoRefresh);
         } finally {
            setQueryRunning(false); // XXX do we need this? its called in finishQuery anyway
            task = null;
         }

      }


      private void setQueryRunning(final boolean running) {
         EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
               queryPanel.setQueryRunning(running);
            }

         });
      }


      @Override
      public void run() {
         startQuery();
         try {
            executeQuery();
         } finally {
            finnishQuery();
         }
      }


      synchronized void post(boolean autoRefresh) {
         if (task != null) {
            task.cancel();
         }
         task = rp.create(this);
         this.autoRefresh = autoRefresh;
         task.schedule(0);
      }


      @Override
      public boolean cancel() {
         if (task != null) {
            task.cancel();
            finnishQuery();
         }
         return true;
      }


      @Override
      public void notifyData(final Issue issue) {
         if (!query.contains(issue)) {
            // XXX this is quite ugly - the query notifies an archoived issue
            // but it doesn't "contain" it!
            return;
         }
         setIssueCount(++counter);
         if (counter == 1) {
            EventQueue.invokeLater(new Runnable() {

               @Override
               public void run() {
                  queryPanel.showNoContentPanel(false);;
               }

            });
         }
      }


      @Override
      public void started() {
         counter = 0;
         setIssueCount(counter);
      }


      @Override
      public void finished() {
      }

   }

}