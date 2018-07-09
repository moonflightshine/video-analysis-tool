/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2015  Douglas Brown
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://www.cabrillo.edu/~dbrown/tracker/>.
 */
package org.opensourcephysics.cabrillo.tracker;

import java.beans.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.opensourcephysics.display.Data;
import org.opensourcephysics.media.core.DataTrack;
import org.opensourcephysics.tools.*;

/**
 * A FunctionTool for building particle models.
 */
public class ModelBuilder extends FunctionTool {
	
  private TrackerPanel trackerPanel;
  private JLabel startFrameLabel, endFrameLabel, boosterLabel;
  private ModelFrameSpinner startFrameSpinner, endFrameSpinner;
  private JComboBox boosterDropdown;
	
	/**
	 * Contsructor.
	 * 
	 * @param trackerPanel the TrackerPanel with the models
	 */
	protected ModelBuilder(TrackerPanel trackerPanel) {
		super(trackerPanel);
		this.trackerPanel = trackerPanel;
		
		// create and set toolbar components
		createToolbarComponents();
		setToolbarComponents(new Component[] 
		    {startFrameLabel, startFrameSpinner, endFrameLabel, endFrameSpinner, boosterLabel, boosterDropdown});		
	}
	
	/**
	 * Creates the toolbar components.
	 */
	protected void createToolbarComponents() {
		// create start and end frame spinners
	  Font font = new JSpinner().getFont();
	  int n = trackerPanel.getPlayer().getVideoClip().getFrameCount()-1;
	  FontRenderContext frc = new FontRenderContext(null, false, false);
	  String s = String.valueOf(Math.max(n, 200));
	  TextLayout layout = new TextLayout(s, font, frc);
	  int w = (int)layout.getBounds().getWidth()+4;
	  if (n>1000) w+=3;
		startFrameLabel = new JLabel();
		startFrameLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));
		endFrameLabel = new JLabel();
		endFrameLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 2));
		SpinnerNumberModel model = new SpinnerNumberModel(0, 0, n, 1); // init, min, max, step
		startFrameSpinner = new ModelFrameSpinner(model);
		startFrameSpinner.prefWidth = w;
		model = new SpinnerNumberModel(n, 0, n, 1); // init, min, max, step
		endFrameSpinner = new ModelFrameSpinner(model);
		endFrameSpinner.prefWidth = w;
		
		// create booster label and dropdown
    boosterLabel = new JLabel();
    boosterLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 2));
    boosterDropdown = new JComboBox();
    boosterDropdown.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 0));
    boosterDropdown.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	if (!boosterDropdown.isEnabled()) return;
    	  FunctionPanel panel = getSelectedPanel();
    	  if (panel!=null) {
    	  	ParticleModel part = ((ModelFunctionPanel)panel).model;
    	  	if (!(part instanceof DynamicParticle)) return;
    	  	DynamicParticle model = (DynamicParticle)part;
    	  	
    	  	Object item = boosterDropdown.getSelectedItem();
          if(item!=null) {
          	Object[] array = (Object[])item;
          	PointMass target = (PointMass)array[1]; // null if "none" selected
	      		model.setBooster(target);
	      		if (target!=null) {
		      		Step step = trackerPanel.getSelectedStep();
		      		if (step!=null && step instanceof PositionStep) {
		      			PointMass pm = (PointMass)((PositionStep)step).track;
		      			if (pm==target) {
		      				model.setStartFrame(step.getFrameNumber());
		      			}
		      		}
	      		}
          }
    	  }
      }
    });
    
    DropdownRenderer renderer= new DropdownRenderer();
    boosterDropdown.setRenderer(renderer);
		refreshBoosterDropdown();

    trackerPanel.addPropertyChangeListener("track", new PropertyChangeListener() { //$NON-NLS-1$
    	public void propertyChange(PropertyChangeEvent e) {
    		refreshBoosterDropdown();
    		refreshLayout();
    	}
    });

    setHelpAction(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        TFrame frame = trackerPanel.getTFrame();
        if (frame != null) {
        	ModelFunctionPanel panel = (ModelFunctionPanel)getSelectedPanel();
        	if (panel.model instanceof DynamicSystem) {
        		frame.showHelp("system", 0); //$NON-NLS-1$
        	}
        	else {
        		frame.showHelp("particle", 0); //$NON-NLS-1$
        	}
        }
      }
    });
	}
	
	/**
	 * Refreshes the GUI.
	 */
	@Override
	protected void refreshGUI() {
  	super.refreshGUI();
  	dropdown.setToolTipText(TrackerRes.getString
				("TrackerPanel.ModelBuilder.Spinner.Tooltip")); //$NON-NLS-1$
		String title = TrackerRes.getString("TrackerPanel.ModelBuilder.Title"); //$NON-NLS-1$  
  	FunctionPanel panel = getSelectedPanel();
  	if (panel!=null) {
  		TTrack track = trackerPanel.getTrack(panel.getName());
  		if (track != null) {
  			String type = track.getClass().getSimpleName();
  			title += ": "+TrackerRes.getString(type+".Builder.Title"); //$NON-NLS-1$ //$NON-NLS-2$
  		}
  	}
		setTitle(title);
		if (boosterDropdown!=null) {
	  	boosterDropdown.setToolTipText(TrackerRes.getString
					("TrackerPanel.Dropdown.Booster.Tooltip")); //$NON-NLS-1$
			boosterLabel.setText(TrackerRes.getString
					("TrackerPanel.Label.Booster")); //$NON-NLS-1$
			startFrameLabel.setText(TrackerRes.getString
					("TrackerPanel.Label.ModelStart")); //$NON-NLS-1$
			endFrameLabel.setText(TrackerRes.getString
					("TrackerPanel.Label.ModelEnd")); //$NON-NLS-1$
			startFrameSpinner.setToolTipText(TrackerRes.getString
					("TrackerPanel.Spinner.ModelStart.Tooltip")); //$NON-NLS-1$
			endFrameSpinner.setToolTipText(TrackerRes.getString
					("TrackerPanel.Spinner.ModelEnd.Tooltip")); //$NON-NLS-1$  	  		
			refreshBoosterDropdown();
		}
  }
  
	@Override
  public void setVisible(boolean vis) {
  	super.setVisible(vis);
  	trackerPanel.isModelBuilderVisible = vis;
  }
	
	@Override
  public void setFontLevel(int level) {
		super.setFontLevel(level);
		refreshBoosterDropdown();
		refreshLayout();
	}
	
	/**
   * Gets the TrackerPanel.
   * 
   * @return the TrackerPanel
   */
	public TrackerPanel getTrackerPanel() {
		return trackerPanel;
	}
	
	/**
   * Refreshes the layout to ensure the booster dropdown is fully displayed.
   */
	protected void refreshLayout() {
  	SwingUtilities.invokeLater(new Runnable() {
  		public void run() {
    		validate();
    		refreshGUI();
    		Dimension dim = getSize();
  			int height = Toolkit.getDefaultToolkit().getScreenSize().height;
  			height = Math.min((int)(0.95*height), (int)(550*(1+fontLevel/4.0)));
  			dim.height = height;
  			setSize(dim); 
    		repaint();
  		}
  	});

	}

	/**
   * Refreshes the start and end frame spinners.
   */
  protected void refreshSpinners() {
	  int n = trackerPanel.getPlayer().getVideoClip().getFrameCount()-1;
	  FunctionPanel panel = getSelectedPanel();
	  startFrameSpinner.setEnabled(panel!=null);
	  endFrameSpinner.setEnabled(panel!=null);
	  startFrameLabel.setEnabled(panel!=null);
	  endFrameLabel.setEnabled(panel!=null);
	  int end = n;
	  ParticleModel model = null;
	  if (panel!=null) {
	  	model = ((ModelFunctionPanel)panel).model;
	  	end = Math.min(n, model.getEndFrame());
	  }
	  // following two lines trigger change events
	  ((SpinnerNumberModel)startFrameSpinner.getModel()).setMaximum(n);
	  ((SpinnerNumberModel)endFrameSpinner.getModel()).setMaximum(n);
	  if (model!=null) {
	  	startFrameSpinner.setValue(model.getStartFrame());
	  	endFrameSpinner.setValue(end);
	  }
	  else {
	  	startFrameSpinner.setValue(0);
	  	endFrameSpinner.setValue(n);
	  }
	  Font font = startFrameSpinner.getFont();
	  FontRenderContext frc = new FontRenderContext(null, false, false);
	  String s = String.valueOf(Math.max(n, 200));
	  TextLayout layout = new TextLayout(s, font, frc);
	  int w = (int)layout.getBounds().getWidth()+1;
	  if (n>1000) w+=3;
  	startFrameSpinner.prefWidth = w;
  	endFrameSpinner.prefWidth = w;
  	
	  validate();
  }

  /**
   * Refreshes the booster dropdown.
   */
  protected void refreshBoosterDropdown() {
  	  FunctionPanel panel = getSelectedPanel();
  	  DynamicParticle dynamicModel = null;
  	  if (panel!=null) {
  	  	ParticleModel model = ((ModelFunctionPanel)panel).model;  	  	
  	  	if (model instanceof DynamicParticle) {
  	  		dynamicModel = (DynamicParticle)model;
  	  	}
    	  boosterDropdown.setEnabled(false);  // disabled during refresh to prevent action
    		// refresh boosterDropdown
  	  	String s = TrackerRes.getString("TrackerPanel.Booster.None"); //$NON-NLS-1$
  	    Object[] none = new Object[] {new ShapeIcon(new Rectangle(), 21, 16), null, s};
  	  	Object[] selected = none;
  	  	boolean targetExists = false;
  	    boosterDropdown.removeAllItems();
  	    ArrayList<PointMass> masses = trackerPanel.getDrawables(PointMass.class);
  	    outer: for (PointMass next: masses) {
  	    	if (next==model) continue;
  	    	if (next instanceof DynamicSystem) continue;
  	    	
  	    	String name = next.getName();
   	      Object[] item = new Object[] {next.getFootprint().getIcon(21, 16), next, name};
   	      
   	      // check that next is not a dynamic particle being boosted by selected model
   	      // or part of a system being boosted by selected model
	  	  	if (next instanceof DynamicParticle) {
	  	  		DynamicParticle dynamic = (DynamicParticle)next;
	  	  		if (dynamic.isBoostedBy(model)) continue;
	  	  		if (dynamic.system!=null) {
	  	  			for (DynamicParticle part: dynamic.system.particles) {
	  	  	  		if (part.isBoostedBy(model)) continue outer;
	  	  			}
	  	  		}
	  	  	}
   	      
  	    	if (dynamicModel!=null) {	
  	    		// check that next is not in same dynamic system as selected model
  	    		if (dynamicModel.system!=null && next instanceof DynamicParticle) {
  	  	  		DynamicParticle dynamicNext = (DynamicParticle)next;
  	  	  		if (dynamicNext.system==dynamicModel.system) {
  	  	  			continue outer;
  	  	  		}
  	    		}
  	    		// check if next is selected model's booster
	  	    	if (dynamicModel.modelBooster!=null) {
	  	    		PointMass booster = dynamicModel.modelBooster.booster;
	  	    		// check if next pointmass is the current booster
		  	  		if (booster==next) {
		  	  			selected = item;
		  	  			targetExists = true;
		  	  		}
	  	    	}
  	    	}
  	      boosterDropdown.addItem(item);
  	    }
	      boosterDropdown.addItem(none);
	      boosterDropdown.setSelectedItem(selected);
	      if (dynamicModel!=null && !targetExists) {
	      	dynamicModel.setBooster(null);
	      }
  	  	boolean enable = dynamicModel!=null && !(dynamicModel instanceof DynamicSystem);
    	  boosterLabel.setEnabled(enable);
    	  boosterDropdown.setEnabled(enable);
  	  }
  	  validate();
  }
  
	/**
	 * Sets the startFrameSpinner value.
	 * 
	 * @param frameNumber the frameNumber (int or Integer)
	 */
  protected void setSpinnerStartFrame(Object frameNumber) {
  	startFrameSpinner.setValue(frameNumber);
  }
  		
	/**
	 * Sets the endFrameSpinner value.
	 * 
	 * @param frameNumber the frameNumber (int or Integer)
	 */
  protected void setSpinnerEndFrame(Object frameNumber) {
  	endFrameSpinner.setValue(frameNumber);
  }
  		
	/**
   * An inner class for the particle model start and end frame spinners.
   */
	class ModelFrameSpinner extends JSpinner {

	  protected int prefWidth;

		Integer prevMax;
		SpinnerNumberModel spinModel;
		
		ModelFrameSpinner(SpinnerNumberModel model) {
			super(model);
			spinModel = model;
    	prevMax = (Integer)spinModel.getMaximum();
  		addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
        	ModelFunctionPanel panel = (ModelFunctionPanel)getSelectedPanel();
        	if (panel==null || panel.model==null || panel.model.refreshing)
        		return;
        	// do nothing if max has changed
        	if (prevMax!=spinModel.getMaximum()) {
        		prevMax = (Integer)spinModel.getMaximum();
        		return;
        	}
        	// otherwise set model start or end frame
      		int n = (Integer)getValue();
        	if (ModelFrameSpinner.this==startFrameSpinner)
        		panel.model.setStartFrame(n);
        	else
        		panel.model.setEndFrame(n);
        }
      });
		}
		
		public Dimension getMaximumSize() {
			Dimension dim = super.getMaximumSize();
			dim.width = getMinimumSize().width;
			return dim;
		}
		
		public Dimension getMinimumSize() {
			Dimension dim = super.getMinimumSize();
	    dim.width += prefWidth-getEditor().getPreferredSize().width;
			return dim;
		}
		
		public Dimension getPreferredSize() {
			Dimension dim = super.getPreferredSize();
			dim.width = getMinimumSize().width;
			return dim;
		}
	}

}


