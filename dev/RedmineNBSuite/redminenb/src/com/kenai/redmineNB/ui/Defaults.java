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
package com.kenai.redmineNB.ui;

import com.kenai.redmineNB.RedmineConfig;
import com.kenai.redmineNB.query.ParameterValue;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import javax.swing.*;
import org.netbeans.modules.bugtracking.spi.RepositoryUser;
import org.openide.util.ImageUtilities;
import org.redmine.ta.beans.IssueCategory;
import org.redmine.ta.beans.IssueStatus;
import org.redmine.ta.beans.Tracker;
import org.redmine.ta.beans.Version;


/**
 * RedmineNB UI Defaults.
 *
 * @author Anchialas <anchialas@gmail.com>
 */
public class Defaults {

   public static final Color COLOR_ERROR = new Color(153, 0, 0);
   private final static Color COLOR_TOP = new Color(198, 211, 223);
   private final static Color COLOR_BOTTOM = new Color(235, 235, 235);


   private Defaults() {
      // suppressed for non-instantiability
   }


   public static Icon getIcon(String iconBaseName) {
      return ImageUtilities.loadImageIcon("com/kenai/redmineNB/resources/" + iconBaseName, false);
   }


   public static Graphics2D paintGradient(Graphics2D g2d, int width, int height) {
      g2d.setPaint(new GradientPaint(0, 0, COLOR_TOP, 0, height, COLOR_BOTTOM));
      g2d.fillRect(0, 0, width, height);
      return g2d;
   }


   public static class TrackerLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value instanceof Tracker) {
            value = ((Tracker) value).getName();
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class IssueStatusLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value != null) {
            value = ((IssueStatus) value).getName();
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class IssueCategoryLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value instanceof IssueCategory) {
            value = ((IssueCategory) value).getName();
         } else if (value == null) {
            value = " ";
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class VersionLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value instanceof Version) {
            value = ((Version) value).getName();
         } else if (value == null) {
            value = " ";
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class RepositoryUserLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value instanceof RepositoryUser) {
            value = ((RepositoryUser) value).getFullName();
         } else if (value == null) {
            value = "";
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class ParameterValueLCR extends DefaultListCellRenderer {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         if (value instanceof ParameterValue) {
            value = ((ParameterValue) value).getDisplayName();
            if (value == null) {
               return new JSeparator();
            }
         }
         return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      }

   }


   public static class PriorityLCR extends ParameterValueLCR {

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
         if (c instanceof JLabel) {
            if (value instanceof ParameterValue) {
               String prio = ((ParameterValue) value).getValue();
               ((JLabel) c).setIcon(RedmineConfig.getInstance().getPriorityIcon(prio));
            } else {
               ((JLabel) c).setIcon(null);
            }
         }
         return c;
      }

   }

}