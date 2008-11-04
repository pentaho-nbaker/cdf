/*
 * Copyright 2006 Pentaho Corporation.  All rights reserved.
 * This software was developed by Pentaho Corporation and is provided under the terms
 * of the Mozilla Public License, Version 1.1, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to http://www.mozilla.org/MPL/MPL-1.1.txt. The Original Code is the Pentaho
 * BI Platform.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 *
 * Created Jun 19, 2006
 * @author rtroyer
 */
package org.pentaho.plugin.jfreechart;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.Paint;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Node;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.ui.RectangleEdge;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.core.session.IPentahoSession;
import org.pentaho.messages.Messages;

public class XYSeriesCollectionChartDefinition extends XYSeriesCollection implements XYChartDefinition {

    private static final long serialVersionUID = 1717509132920946530L;
    private int chartType = JFreeChartEngine.UNDEFINED_CHART_TYPE;
    private String noDataMessage = null;

    // JFreeChart Customizations
    private String title = ""; //$NON-NLS-1$
    private RectangleEdge titlePosition = RectangleEdge.TOP;
    private Font titleFont = TextTitle.DEFAULT_FONT;
    private List subTitles = new ArrayList();
    private Paint chartBackgroundPaint = Color.WHITE;
    private Image chartBackgroundImage = null;
    private boolean borderVisible = false;
    private Paint borderPaint = Color.BLACK;
    private int width = 200;
    private int height = 200;
    private int dotWidth = 5;
    private int dotHeight = 5;

    // Plot Customizations
    private PlotOrientation orientation = PlotOrientation.VERTICAL;
    private Paint plotBackgroundPaint = Color.WHITE;
    private Image plotBackgroundImage = null;
    private boolean legendIncluded = true;
    private boolean threeD = false;
    private boolean stacked = false;
    private boolean domainVerticalTickLabels = false;
    private boolean domainIncludesZero = true;
    private boolean domainStickyZero = true;
    private boolean rangeIncludesZero = true;
    private boolean rangeStickyZero = true;
    private Paint[] paintSequence = null;
    private String domainTitle = null;
    private Font domainTitleFont = TextTitle.DEFAULT_FONT;
    private Font domainTickFont = null;
    private NumberFormat domainTickFormat = null;
    private double domainMinimum = ValueAxis.DEFAULT_LOWER_BOUND;
    private double domainMaximum = ValueAxis.DEFAULT_UPPER_BOUND;
    private String rangeTitle = null;
    private Font rangeTitleFont = TextTitle.DEFAULT_FONT;
    private Font rangeTickFont = null;
    private NumberFormat rangeTickFormat = null;
    private double rangeMinimum = ValueAxis.DEFAULT_LOWER_BOUND;
    private double rangeMaximum = ValueAxis.DEFAULT_UPPER_BOUND;
    private Font legendFont = null;
    private boolean legendBorderVisible = true;
    private String lineStyle = ChartDefinition.LINE_STYLE_SOLID_STR;
    private float lineWidth = 1.0f;
    private boolean markersVisible = false;
    private Float backgroundAlpha;
    private Float foregroundAlpha;
    
    // in JFreeChart, the tokens stand for:
    // {0} - the series name
    // {1} - the preformatted x-value
    // {2} - the preformatted y-value
    private String tooltipContent = "{1}, {2}"; //$NON-NLS-1$
    private String tooltipYFormat = "0"; //$NON-NLS-1$
    private String tooltipXFormat = "0"; //$NON-NLS-1$

    // Other stuff
    private IPentahoSession session;

    public XYSeriesCollectionChartDefinition(IPentahoSession session) {
        super();
        this.session = session;
    }

    public XYSeriesCollectionChartDefinition(int chartType, IPentahoResultSet data, boolean byRow, IPentahoSession session) {
        this(session);
        this.chartType = chartType;
        if (byRow) {
            setDataByRow(data);
        } else {
            setDataByColumn(data);
        }
    }

    public XYSeriesCollectionChartDefinition(IPentahoResultSet data, boolean byRow, Node chartAttributes,
        IPentahoSession session) {
        this(JFreeChartEngine.UNDEFINED_CHART_TYPE, data, byRow, session);
        setChartAttributes(chartAttributes);
    }

    public static Log getLogger() {
        return LogFactory.getLog(XYSeriesCollectionChartDefinition.class);
    }

    private void setChartAttributes(Node chartAttributes) {
        if (chartAttributes == null) {
            return;
        }
        // get the chart type from the chart node -- this overrides the current
        // chart type
        setChartType(chartAttributes.selectSingleNode(TYPE_NODE_NAME));

        // set the chart background
        setChartBackground(chartAttributes.selectSingleNode(CHART_BACKGROUND_NODE_NAME));

        // set the plot background
        setPlotBackground(chartAttributes.selectSingleNode(PLOT_BACKGROUND_NODE_NAME));

        // set the orientation
        setOrientation(chartAttributes.selectSingleNode(ORIENTATION_NODE_NAME));

        // do we want a legend
        setLegendIncluded(chartAttributes.selectSingleNode(INCLUDE_LEGEND_NODE_NAME));

        // get the chart title
        setTitle(chartAttributes.selectSingleNode(TITLE_NODE_NAME));

        // set the alfa layers
        setBackgroundAlpha(chartAttributes.selectSingleNode(BACKGROUND_ALPHA_NODE_NAME));

        setForegroundAlpha(chartAttributes.selectSingleNode(FOREGROUND_ALPHA_NODE_NAME));

        
        // get the chart subtitles

        // A list of <subtitle> nodes should not be allowed to exist as a child of the main XML element (for XML schema to 
        // be well constructed and validate the XML . 
        // We have deprecated <subtitle> as a child of the main node , and now require a <subtitles> parent node 
        // under which <subtitle> can exist. 

        List subtitles = chartAttributes.selectNodes(SUBTITLE_NODE_NAME);

        if ((subtitles == null) || (subtitles.isEmpty())) {
            Node subTitlesNode = chartAttributes.selectSingleNode(SUBTITLES_NODE_NAME);
            if (subTitlesNode != null) {
                subtitles = subTitlesNode.selectNodes(SUBTITLE_NODE_NAME);
            }
        } else {
            // log a deprecation warning for this property...
            getLogger().warn(Messages.getString("CHART.WARN_DEPRECATED_CHILD", SUBTITLE_NODE_NAME, SUBTITLES_NODE_NAME));//$NON-NLS-1$ 
            getLogger().warn(Messages.getString("CHART.WARN_PROPERTY_WILL_NOT_VALIDATE", SUBTITLE_NODE_NAME));//$NON-NLS-1$  
        }

        if (subtitles != null) {
            addSubTitles(subtitles);
        }


        // get the paint sequence
        setPaintSequence(chartAttributes.selectSingleNode(PALETTE_NODE_NAME));

        // get the stacked value
        setStacked(chartAttributes.selectSingleNode(STACKED_NODE_NAME));

        // get the 3D value
        setThreeD(chartAttributes.selectSingleNode(THREED_NODE_NAME));

        // set the width
        setWidth(chartAttributes.selectSingleNode(WIDTH_NODE_NAME));

        // set the height
        setHeight(chartAttributes.selectSingleNode(HEIGHT_NODE_NAME));

        // set the dot width
        setDotWidth(chartAttributes.selectSingleNode(DOT_WIDTH_NODE_NAME));

        // set the dot height
        setDotHeight(chartAttributes.selectSingleNode(DOT_HEIGHT_NODE_NAME));

        // set vertical tick labels flag
        setDomainVerticalTickLabels(chartAttributes.selectSingleNode(DOMAIN_VERTICAL_TICK_LABELS_NODE_NAME));

        // set the border on or off
        setBorderVisible(chartAttributes.selectSingleNode(CHART_BORDER_VISIBLE_NODE_NAME));

        // set the border Paint
        setBorderPaint(JFreeChartEngine.getPaint(chartAttributes.selectSingleNode(CHART_BORDER_PAINT_NODE_NAME)));

        // set the title location
        setTitlePosition(chartAttributes.selectSingleNode(TITLE_POSITION_NODE_NAME));

        // set the title font
        setTitleFont(chartAttributes.selectSingleNode(TITLE_FONT_NODE_NAME));

        // set the domain title
        setDomainTitle(chartAttributes.selectSingleNode(DOMAIN_TITLE_NODE_NAME));

        //set the domain title font
        setDomainTitleFont(chartAttributes.selectSingleNode(DOMAIN_TITLE_FONT_NODE_NAME));

        //set the domain tick font
        setDomainTickFont(chartAttributes.selectSingleNode(DOMAIN_TICK_FONT_NODE_NAME));

        //the the domain tick label number format
        setDomainTickFormat(chartAttributes.selectSingleNode(DOMAIN_TICK_FORMAT_NODE_NAME));

        // set the range title
        setRangeTitle(chartAttributes.selectSingleNode(RANGE_TITLE_NODE_NAME));

        // the the range title font
        setRangeTitleFont(chartAttributes.selectSingleNode(RANGE_TITLE_FONT_NODE_NAME));

        //the the range tick font
        setRangeTickFont(chartAttributes.selectSingleNode(RANGE_TICK_FONT_NODE_NAME));

        //the the range tick label number format
        setRangeTickFormat(chartAttributes.selectSingleNode(RANGE_TICK_FORMAT_NODE_NAME));

        // set the domain minimum
        setDomainMinimum(chartAttributes.selectSingleNode(DOMAIN_MINIMUM_NODE_NAME));

        //set the domain minimum
        setDomainMaximum(chartAttributes.selectSingleNode(DOMAIN_MAXIMUM_NODE_NAME));

        //set the range minimum
        setRangeMinimum(chartAttributes.selectSingleNode(RANGE_MINIMUM_NODE_NAME));

        //set the range minimum
        setRangeMaximum(chartAttributes.selectSingleNode(RANGE_MAXIMUM_NODE_NAME));

        // set we want domain to include zero
        setDomainIncludesZero(chartAttributes.selectSingleNode(DOMAIN_INCLUDES_ZERO_NODE_NAME));

        // set we want domain to force zero
        setDomainStickyZero(chartAttributes.selectSingleNode(DOMAIN_STICKY_ZERO_NODE_NAME));

        // set we want domain to include zero
        setRangeIncludesZero(chartAttributes.selectSingleNode(RANGE_INCLUDES_ZERO_NODE_NAME));

        // set we want domain to force zero
        setRangeStickyZero(chartAttributes.selectSingleNode(RANGE_STICKY_ZERO_NODE_NAME));

        // set the line style
        setLineStyle(chartAttributes.selectSingleNode(LINE_STYLE_NODE_NAME));

        // set the line width
        setLineWidth(chartAttributes.selectSingleNode(LINE_WIDTH_NODE_NAME));

        // set the marker visibility
        setMarkersVisible(chartAttributes.selectSingleNode(MARKER_VISIBLE_NODE_NAME));

        //set legend font
        setLegendFont(chartAttributes.selectSingleNode(LEGEND_FONT_NODE_NAME));

        // set legend border visible
        setLegendBorderVisible(chartAttributes.selectSingleNode(DISPLAY_LEGEND_BORDER_NODE_NAME));

        setTooltipContent(chartAttributes.selectSingleNode(TOOLTIP_CONTENT_NODE_NAME));

        setTooltipYFormat(chartAttributes.selectSingleNode(TOOLTIP_Y_FORMAT_NODE_NAME));

        setTooltipXFormat(chartAttributes.selectSingleNode(TOOLTIP_X_FORMAT_NODE_NAME));

    }

    private void setDataByColumn(IPentahoResultSet data) {
        // TODO Make this routine MDX friendly
        if (data == null) {
            noDataMessage = Messages.getString("CHART.USER_NO_DATA_AVAILABLE"); //$NON-NLS-1$
            return; // No data so we've got nothing to set
        // TODO come up with some sort of error strategy here.
        }
        boolean firstPass = true;
        String lastSeries = ""; //$NON-NLS-1$
        String seriesName = ""; //$NON-NLS-1$
        Object[] rowData = data.next();
        XYSeries wrkSeries = new XYSeries(seriesName);
        while (rowData != null) {
            seriesName = rowData[0].toString();
            if (firstPass || !seriesName.equalsIgnoreCase(lastSeries)) {
                if (!firstPass) {
                    addSeries(wrkSeries);
                }
                wrkSeries = new XYSeries(seriesName);
                lastSeries = seriesName;
                firstPass = false;
            }
            if (rowData[1] instanceof Number && rowData[2] instanceof Number) {
                wrkSeries.add(((Number) rowData[1]).doubleValue(), ((Number) rowData[2]).doubleValue());
            }
            rowData = data.next();
        }

        if (!firstPass) {
            addSeries(wrkSeries);
        }

        if ((data.getRowCount() > 0) && (this.getSeriesCount() <= 0)) {
            noDataMessage = Messages.getString("CHART.USER_INCORRECT_DATA_FORMAT"); //$NON-NLS-1$
        }

    }

    private void setDataByRow(IPentahoResultSet data) {
        // TODO Make this routine MDX friendly
        if (data == null) {
            noDataMessage = Messages.getString("CHART.USER_NO_DATA_AVAILABLE"); //$NON-NLS-1$
            return; // No data so we've got nothing to set
        // TODO come up with some sort of error strategy here.
        }
        Object[] rowData = data.next();
        while (rowData != null) {
            String seriesName = (String) rowData[0];
            XYSeries wrkSeries = new XYSeries(seriesName);
            for (int column = 1; column < rowData.length - 1; column = column + 2) {
                wrkSeries.add(((Number) rowData[column]).doubleValue(), ((Number) rowData[column + 1]).doubleValue());
            }
            addSeries(wrkSeries);
            rowData = data.next();
        }
        if ((data.getRowCount() > 0) && (this.getSeriesCount() <= 0)) {
            noDataMessage = Messages.getString("CHART.USER_INCORRECT_DATA_FORMAT"); //$NON-NLS-1$
        }
    }

    /**
     * @param backgroundPaint
     *            The backgroundPaint to set.
     */
    public void setChartBackgroundPaint(Paint chartBackgroundPaint) {
        if (chartBackgroundPaint != null) {
            this.chartBackgroundPaint = chartBackgroundPaint;
        }
    }

    /**
     * Return the java.awt.Font to be used to display the dial title
     *
     * @return Font The Font for the title of this Pie
     */
    public Font getTitleFont() {
        return titleFont;
    }

    public void setTitleFont(Font titleFont) {
        this.titleFont = titleFont;
    }

    public void setTitleFont(Node titleFontNode) {
        Font font = JFreeChartEngine.getFont(titleFontNode);
        if (font != null) {
            setTitleFont(font);
        }
    }

    /**
     * @return Returns the backgroundPaint.
     */
    public Paint getChartBackgroundPaint() {
        return chartBackgroundPaint;
    }

    /**
     * @return Returns the chartType.
     */
    public int getChartType() {
        return chartType;
    }

    public static int getChartType(String typeStr) {
        if (typeStr != null) {
            if (LINE_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.LINE_CHART_TYPE;
            } else if (AREA_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.AREA_CHART_TYPE;
            } else if (STEP_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.STEP_CHART_TYPE;
            } else if (STEP_AREA_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.STEP_AREA_CHART_TYPE;
            } else if (DIFFERENCE_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.DIFFERENCE_CHART_TYPE;
            } else if (DOT_CHART_STR.equalsIgnoreCase(typeStr)) {
                return JFreeChartEngine.DOT_CHART_TYPE;
            }
        }
        return JFreeChartEngine.UNDEFINED_CHART_TYPE;
    }

    public void setChartType(Node chartTypeNode) {
        if (chartTypeNode != null) {
            String typeStr = chartTypeNode.getText();
            setChartType(getChartType(typeStr));
        }
    }

    /**
     * @param chartType
     *            The chartType to set.
     */
    public void setChartType(int chartType) {
        this.chartType = chartType;
    }

    /**
     * @return Returns the threeD.
     */
    public boolean isThreeD() {
        return threeD;
    }

    public void setThreeD(Node threeDNode) {
        if (threeDNode != null) {
            String boolStr = threeDNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setThreeD(booleanValue.booleanValue());
        }
    }

    /**
     * @param threeD
     *            The threeD to set.
     */
    public void setThreeD(boolean threeD) {
        this.threeD = threeD;
    }

    /**
     * @return Returns the stacked.
     */
    public boolean isStacked() {
        return stacked;
    }

    public void setStacked(Node stackedNode) {
        if (stackedNode != null) {
            String boolStr = stackedNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setStacked(booleanValue.booleanValue());
        }
    }

    /**
     * @param stacked
     *            The stacked to set.
     */
    public void setStacked(boolean stacked) {
        this.stacked = stacked;
    }

    /**
     * @return Returns the verticalTickLabels.
     */
    public boolean isDomainVerticalTickLabels() {
        return domainVerticalTickLabels;
    }

    public void setDomainVerticalTickLabels(Node domainVerticalTickLabelsNode) {
        if (domainVerticalTickLabelsNode != null) {
            String boolStr = domainVerticalTickLabelsNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setDomainVerticalTickLabels(booleanValue.booleanValue());
        }
    }

    /**
     * @param domainVerticalTickLabels
     *            The domainVerticalLabels to set.
     */
    public void setDomainVerticalTickLabels(boolean domainVerticalTickLabels) {
        this.domainVerticalTickLabels = domainVerticalTickLabels;
    }

    /**
     * @return Returns the domainIncludeZero.
     */
    public boolean isDomainIncludesZero() {
        return domainIncludesZero;
    }

    public void setDomainIncludesZero(Node domainIncludesZeroNode) {
        if (domainIncludesZeroNode != null) {
            String boolStr = domainIncludesZeroNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setDomainIncludesZero(booleanValue.booleanValue());
        }
    }

    /**
     * @param domainIncludesZero
     *            The domainIncludesZero to set.
     */
    public void setDomainIncludesZero(boolean domainIncludesZero) {
        this.domainIncludesZero = domainIncludesZero;
    }

    /**
     * @return Returns the domainStickyZero.
     */
    public boolean isDomainStickyZero() {
        return domainStickyZero;
    }

    public void setDomainStickyZero(Node domainStickyZeroNode) {
        if (domainStickyZeroNode != null) {
            String boolStr = domainStickyZeroNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setDomainStickyZero(booleanValue.booleanValue());
        }
    }

    /**
     * @param domainStickyZero
     *            The domainStickyZero to set.
     */
    public void setDomainStickyZero(boolean domainStickyZero) {
        this.domainStickyZero = domainStickyZero;
    }

    /**
     * @return Returns the domainIncludeZero.
     */
    public boolean isRangeIncludesZero() {
        return rangeIncludesZero;
    }

    public void setRangeIncludesZero(Node rangeIncludesZeroNode) {
        if (rangeIncludesZeroNode != null) {
            String boolStr = rangeIncludesZeroNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setRangeIncludesZero(booleanValue.booleanValue());
        }
    }

    /**
     * @param domainIncludesZero
     *            The domainIncludesZero to set.
     */
    public void setRangeIncludesZero(boolean rangeIncludesZero) {
        this.rangeIncludesZero = rangeIncludesZero;
    }

    /**
     * @return Returns the domainStickyZero.
     */
    public boolean isRangeStickyZero() {
        return rangeStickyZero;
    }

    public void setRangeStickyZero(Node rangeStickyZeroNode) {
        if (rangeStickyZeroNode != null) {
            String boolStr = rangeStickyZeroNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setRangeStickyZero(booleanValue.booleanValue());
        }
    }

    /**
     * @param domainStickyZero
     *            The domainStickyZero to set.
     */
    public void setRangeStickyZero(boolean rangeStickyZero) {
        this.rangeStickyZero = rangeStickyZero;
    }

    /**
     * @return Returns the height.
     */
    public int getHeight() {
        return height;
    }

    public void setHeight(Node heightNode) {
        if (heightNode != null) {
            setHeight(Integer.parseInt(heightNode.getText()));
        }
    }

    /**
     * @param height
     *            The height to set.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return Returns the width.
     */
    public int getWidth() {
        return width;
    }

    public void setWidth(Node widthNode) {
        if (widthNode != null) {
            setWidth(Integer.parseInt(widthNode.getText()));
        }
    }

    /**
     * @param width
     *            The width to set.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    //--------------------------------------------------
    /**
     * @return Returns the dot height.
     */
    public int getDotHeight() {
        return dotHeight;
    }

    public void setDotHeight(Node heightNode) {
        if (heightNode != null) {
            setDotHeight(Integer.parseInt(heightNode.getText()));
        }
    }

    /**
     * @param height
     *            The dot height to set.
     */
    public void setDotHeight(int height) {
        this.dotHeight = height;
    }

    /**
     * @return Returns the dot width.
     */
    public int getDotWidth() {
        return dotWidth;
    }

    public void setDotWidth(Node widthNode) {
        if (widthNode != null) {
            setDotWidth(Integer.parseInt(widthNode.getText()));
        }
    }

    /**
     * @param width
     *            The dot width to set.
     */
    public void setDotWidth(int width) {
        this.dotWidth = width;
    }

    //--------------------------------------------------
    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(Node chartTitleNode) {
        if (chartTitleNode != null) {
            setTitle(chartTitleNode.getText());
        }
    }

    /**
     * @param title
     *            The title to set.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return Returns the paintSequence.
     */
    public Paint[] getPaintSequence() {
        return paintSequence;
    }

    public void setPaintSequence(Node paletteNode) {
        if (paletteNode != null) {
            List colorNodes = paletteNode.selectNodes(COLOR_NODE_NAME);
            Paint[] paints = new Paint[colorNodes.size()];
            for (int i = 0; i < colorNodes.size(); i++) {
                paints[i] = JFreeChartEngine.getPaint((Node) colorNodes.get(i));
            }
            setPaintSequence(paints);
        }
    }

    /**
     * @param paintSequence
     *            The paintSequence to set.
     */
    public void setPaintSequence(Paint[] paintSequence) {
        this.paintSequence = paintSequence;
    }

    /**
     * @return Returns the subTitles.
     */
    public List getSubtitles() {
        return subTitles;
    }

    public void addSubTitles(List subTitleNodes) {
        if (subTitleNodes != null) {
            Iterator iter = subTitleNodes.iterator();
            while (iter.hasNext()) {
                addSubTitle(((Node) iter.next()).getText());
            }
        }
    }

    public void addSubTitle(String subTitle) {
        subTitles.add(subTitle);
    }

    /**
     * @return Returns the chartBackgroundImage.
     */
    public Image getChartBackgroundImage() {
        return chartBackgroundImage;
    }

    public void setChartBackgroundImage(Node chartBackgroundImageNode) {
        setChartBackgroundImage(JFreeChartEngine.getImage(chartBackgroundImageNode, getSession()));
    }

    /**
     * @param chartBackgroundImage
     *            The chartBackgroundImage to set.
     */
    public void setChartBackgroundImage(Image chartBackgroundImage) {
        this.chartBackgroundImage = chartBackgroundImage;
    }

    /**
     * @return Returns the legendIncluded.
     */
    public boolean isLegendIncluded() {
        return legendIncluded;
    }

    public void setLegendIncluded(Node legendNode) {
        if (legendNode != null) {
            String boolStr = legendNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setLegendIncluded(booleanValue.booleanValue());
        }
    }

    /**
     * @param legendIncluded
     *            The legendIncluded to set.
     */
    public void setLegendIncluded(boolean legendIncluded) {
        this.legendIncluded = legendIncluded;
    }

    public void setPlotBackgroundPaint(Paint plotBackgroundPaint) {
        if (plotBackgroundPaint != null) {
            this.plotBackgroundPaint = plotBackgroundPaint;
        }
    }

    public Paint getPlotBackgroundPaint() {
        return plotBackgroundPaint;
    }

    /**
     * @return Returns the plotBackgroundImage.
     */
    public Image getPlotBackgroundImage() {
        return plotBackgroundImage;
    }

    public void setPlotBackgroundImage(Node plotBackgroundImageNode) {
        setPlotBackgroundImage(JFreeChartEngine.getImage(plotBackgroundImageNode, getSession()));
    }

    /**
     * @param plotBackgroundImage
     *            The plotBackgroundImage to set.
     */
    public void setPlotBackgroundImage(Image plotBackgroundImage) {
        this.plotBackgroundImage = plotBackgroundImage;
    }

    /**
     * @return Returns the orientation.
     */
    public PlotOrientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Node orientationNode) {
        if (orientationNode != null) {
            String orientationStr = orientationNode.getText();
            if (VERTICAL_ORIENTATION.equalsIgnoreCase(orientationStr)) {
                setOrientation(PlotOrientation.VERTICAL);
            } else if (HORIZONTAL_ORIENTATION.equalsIgnoreCase(orientationStr)) {
                setOrientation(PlotOrientation.HORIZONTAL);
            }
        }
    }

    /**
     * @param orientation
     *            The orientation to set.
     */
    public void setOrientation(PlotOrientation orientation) {
        this.orientation = orientation;
    }

    /**
     * @return Returns the borderVisible.
     */
    public boolean isBorderVisible() {
        return borderVisible;
    }

    public void setBorderVisible(Node borderVisibleNode) {
        if (borderVisibleNode != null) {
            String boolStr = borderVisibleNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setBorderVisible(booleanValue.booleanValue());
        }
    }

    /**
     * @param borderVisible
     *            The borderVisible to set.
     */
    public void setBorderVisible(boolean borderVisible) {
        this.borderVisible = borderVisible;
    }

    /**
     * @return Returns the borderPaint.
     */
    public Paint getBorderPaint() {
        return borderPaint;
    }

    /**
     * @param borderPaint
     *            The borderPaint to set.
     */
    public void setBorderPaint(Paint borderPaint) {
        this.borderPaint = borderPaint;
    }

    public void setTitlePosition(Node titlePositionNode) {
        if (titlePositionNode != null) {
            String titlePositionStr = titlePositionNode.getText();
            if ("top".equalsIgnoreCase(titlePositionStr)) { //$NON-NLS-1$
                setTitlePosition(RectangleEdge.TOP);
            } else if ("left".equalsIgnoreCase(titlePositionStr)) { //$NON-NLS-1$
                setTitlePosition(RectangleEdge.LEFT);
            } else if ("bottom".equalsIgnoreCase(titlePositionStr)) { //$NON-NLS-1$
                setTitlePosition(RectangleEdge.BOTTOM);
            } else if ("right".equalsIgnoreCase(titlePositionStr)) { //$NON-NLS-1$
                setTitlePosition(RectangleEdge.RIGHT);
            }
        }
    }

    /**
     * @return Returns the titlePosition.
     */
    public RectangleEdge getTitlePosition() {
        return titlePosition;
    }

    /**
     * @param titlePosition
     *            The titlePosition to set.
     */
    public void setTitlePosition(RectangleEdge titlePosition) {
        this.titlePosition = titlePosition;
    }

    public void setChartBackground(Node chartBackgroundNode) {
        if (chartBackgroundNode != null) {
            Node backgroundTypeNode = chartBackgroundNode.selectSingleNode(BACKGROUND_TYPE_ATTRIBUTE_NAME);
            if (backgroundTypeNode != null) {
                String backgroundTypeStr = backgroundTypeNode.getText();
                if (COLOR_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setChartBackgroundPaint(JFreeChartEngine.getPaint(chartBackgroundNode));
                    setChartBackgroundImage((Image) null);
                } else if (IMAGE_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setChartBackgroundImage(chartBackgroundNode);
                    setChartBackgroundPaint(null);
                } else if (TEXTURE_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setChartBackgroundPaint(JFreeChartEngine.getTexturePaint(chartBackgroundNode, getWidth(), getHeight(),
                        getSession()));
                    setChartBackgroundImage((Image) null);
                } else if (GRADIENT_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setChartBackgroundPaint(JFreeChartEngine.getGradientPaint(chartBackgroundNode, getWidth(), getHeight()));
                    setChartBackgroundImage((Image) null);
                }
            }
        }
    }

    public void setPlotBackground(Node plotBackgroundNode) {
        if (plotBackgroundNode != null) {
            Node backgroundTypeNode = plotBackgroundNode.selectSingleNode(BACKGROUND_TYPE_ATTRIBUTE_NAME);
            if (backgroundTypeNode != null) {
                String backgroundTypeStr = backgroundTypeNode.getText();
                if (COLOR_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setPlotBackgroundPaint(JFreeChartEngine.getPaint(plotBackgroundNode));
                    setPlotBackgroundImage((Image) null);
                } else if (IMAGE_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setPlotBackgroundImage(plotBackgroundNode);
                    setPlotBackgroundPaint(null);
                } else if (TEXTURE_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setPlotBackgroundPaint(JFreeChartEngine.getTexturePaint(plotBackgroundNode, getWidth(), getHeight(),
                        getSession()));
                    setPlotBackgroundImage((Image) null);
                } else if (GRADIENT_TYPE_NAME.equalsIgnoreCase(backgroundTypeStr)) {
                    setPlotBackgroundPaint(JFreeChartEngine.getGradientPaint(plotBackgroundNode, getWidth(), getHeight()));
                    setPlotBackgroundImage((Image) null);
                }
            }
        }
    }

    public void setDomainTitle(Node titleNode) {
        if (titleNode != null) {
            setDomainTitle(titleNode.getText());
        }
    }

    /**
     * @return Returns the domainTitle.
     */
    public String getDomainTitle() {
        return domainTitle;
    }

    /**
     * @param domainTitle
     *            The domainTitle to set.
     */
    public void setDomainTitle(String domainTitle) {
        this.domainTitle = domainTitle;
    }

    public void setRangeTitle(Node titleNode) {
        if (titleNode != null) {
            setRangeTitle(titleNode.getText());
        }
    }

    /**
     * @return Returns the rangeTitle.
     */
    public String getRangeTitle() {
        return rangeTitle;
    }

    /**
     * @param rangeTitle
     *            The rangeTitle to set.
     */
    public void setRangeTitle(String rangeTitle) {
        this.rangeTitle = rangeTitle;
    }

    public void setDomainTitleFont(Node titleFontNode) {
        Font font = JFreeChartEngine.getFont(titleFontNode);
        if (font != null) {
            setDomainTitleFont(font);
        }
    }

    /**
     * @return Returns the domainTitleFont.
     */
    public Font getDomainTitleFont() {
        return domainTitleFont;
    }

    /**
     * @param domainTitleFont
     *            The domainTitleFont to set.
     */
    public void setDomainTitleFont(Font domainTitleFont) {
        this.domainTitleFont = domainTitleFont;
    }

    /**
     * Return the java.awt.Font to be used to display the range axis tick labels
     *
     * @return Font The Font for the range axis tick labels
     */
    public Font getDomainTickFont() {
        return domainTickFont;
    }

    /**
     * @param domainTickFont
     *            The domainTickFont to set.
     */
    public void setDomainTickFont(Font domainTickFont) {
        this.domainTickFont = domainTickFont;
    }

    public void setDomainTickFont(Node rangeTickFontNode) {
        Font font = JFreeChartEngine.getFont(rangeTickFontNode);
        if (font != null) {
            setDomainTickFont(font);
        }
    }

    /**
     * @return Returns the rangeTickFormat.
     */
    public NumberFormat getDomainTickFormat() {
        return domainTickFormat;
    }

    /**
     * @param rangeTickFormat
     *            The range tick number format to set.
     */
    public void setDomainTickFormat(NumberFormat domainTickFormat) {
        this.domainTickFormat = domainTickFormat;
    }

    public void setDomainTickFormat(Node tickFormatFontNode) {
        if (tickFormatFontNode != null) {
            NumberFormat format = new DecimalFormat(tickFormatFontNode.getText());
            if (format != null) {
                setDomainTickFormat(format);
            }
        }
    }

    public void setRangeTitleFont(Node titleFontNode) {
        Font font = JFreeChartEngine.getFont(titleFontNode);
        if (font != null) {
            setRangeTitleFont(font);
        }
    }

    /**
     * @return Returns the rangeTitleFont.
     */
    public Font getRangeTitleFont() {
        return rangeTitleFont;
    }

    /**
     * @param rangeTitleFont
     *            The rangeTitleFont to set.
     */
    public void setRangeTitleFont(Font rangeTitleFont) {
        this.rangeTitleFont = rangeTitleFont;
    }

    /**
     * @return Returns the rangeTickFormat.
     */
    public NumberFormat getRangeTickFormat() {
        return rangeTickFormat;
    }

    /**
     * @param rangeTickFormat
     *            The range tick number format to set.
     */
    public void setRangeTickFormat(NumberFormat rangeTickFormat) {
        this.rangeTickFormat = rangeTickFormat;
    }

    public void setRangeTickFormat(Node tickFormatFontNode) {
        if (tickFormatFontNode != null) {
            NumberFormat format = new DecimalFormat(tickFormatFontNode.getText());
            if (format != null) {
                setRangeTickFormat(format);
            }
        }
    }

    /**
     * Return the java.awt.Font to be used to display the range axis tick labels
     *
     * @return Font The Font for the range axis tick labels
     */
    public Font getRangeTickFont() {
        return rangeTickFont;
    }

    /**
     * @param rangeTitleFont
     *            The rangeTitleFont to set.
     */
    public void setRangeTickFont(Font rangeTickFont) {
        this.rangeTickFont = rangeTickFont;
    }

    public void setRangeTickFont(Node rangeTickFontNode) {
        Font font = JFreeChartEngine.getFont(rangeTickFontNode);
        if (font != null) {
            setRangeTickFont(font);
        }
    }

    public boolean isDisplayLabels() {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     *
     * @return returns the style set for the lines
     * @see ChartDefinition.LINE_STYLE_SOLID_STR
     * @see ChartDefinition.LINE_STYLE_DASH_STR
     * @see ChartDefinition.LINE_STYLE_DOT_STR
     * @see ChartDefinition.LINE_STYLE_DASHDOT_STR
     * @see ChartDefinition.LINE_STYLE_DASHDOTDOT_STR
     */
    public String getLineStyle() {
        return lineStyle;
    }

    /**
     *
     * @param lineStyle set the style for all line series
     * @see ChartDefinition.LINE_STYLE_SOLID_STR
     * @see ChartDefinition.LINE_STYLE_DASH_STR
     * @see ChartDefinition.LINE_STYLE_DOT_STR
     * @see ChartDefinition.LINE_STYLE_DASHDOT_STR
     * @see ChartDefinition.LINE_STYLE_DASHDOTDOT_STR
     */
    public void setLineStyle(String lineStyle) {
        this.lineStyle = lineStyle;
    }

    /**
     *
     * @param lineStyleNode set the style from an XML node
     */
    public void setLineStyle(Node lineStyleNode) {
        if (lineStyleNode != null) {
            setLineStyle(lineStyleNode.getText());
        }
    }

    /**
     *
     * @return the width of all line series
     * Valid values are float numbers zero or greater
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     *
     * @param lineWidth set the width of all line series
     * Valid values are float numbers zero or greater
     */
    public void setLineWidth(float lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     *
     * @param lineWidthNode set the line width from an XML node
     */
    public void setLineWidth(Node lineWidthNode) {
        if (lineWidthNode != null) {
            setLineWidth(Float.parseFloat(lineWidthNode.getText()));
        }
    }

    public IPentahoSession getSession() {
        return session;
    }

    public void setSession(IPentahoSession session) {
        this.session = session;
    }

    /**
     *
     * @return boolean whether the markers (data points) for all series are displayed
     */
    public boolean isMarkersVisible() {
        return markersVisible;
    }

    /**
     *
     * @param markersVisible set whether the markers (data points) for all series should be displayed
     */
    public void setMarkersVisible(boolean markersVisible) {
        this.markersVisible = markersVisible;
    }

    /**
     *
     * @param markersVisibleNode set the markers visibility from an XML node
     */
    public void setMarkersVisible(Node markersVisibleNode) {
        if (markersVisibleNode != null) {
            String boolStr = markersVisibleNode.getText();
            Boolean booleanValue = new Boolean(boolStr);
            setMarkersVisible(booleanValue.booleanValue());
        }
    }

    /**
     * Return the java.awt.Font to be used to display the legend items
     *
     * @return Font The font for the legend items
     */
    public Font getLegendFont() {
        // TODO Auto-generated method stub
        return legendFont;
    }

    /**
     * Set java.awt.Font to be used to display the legend items
     *
     * @param Font The java.awt.Font for the legend items
     */
    public void setLegendFont(Font legendFont) {
        this.legendFont = legendFont;
    }

    public void setLegendFont(Node legendFontNode) {
        Font font = JFreeChartEngine.getFont(legendFontNode);
        if (font != null) {
            setLegendFont(font);
        }
    }

    public void setLegendBorderVisible(Node legendBorderVisibleNode) {
        if (legendBorderVisibleNode != null) {
            boolean legBorderVisible = (new Boolean(legendBorderVisibleNode.getText())).booleanValue();
            setLegendBorderVisible(legBorderVisible);
        }
    }

    /**
     * @param boolean legendBorderVisible
     *        Set the visibility of the legend border.
     */
    public void setLegendBorderVisible(boolean legendBorderVisible) {
        this.legendBorderVisible = legendBorderVisible;
    }

    /**
     * Return the boolen that states if the legend border is visible
     *
     * @return boolean Is the legend border visible
     */
    public boolean isLegendBorderVisible() {
        // TODO Auto-generated method stub
        return legendBorderVisible;
    }

    /**
     * Return the range axis' minimum value
     *
     * @return double Range axis' minimum value
     */
    public double getRangeMinimum() {
        return rangeMinimum;
    }

    public void setRangeMinimum(Node rangeMinimumNode) {
        if (rangeMinimumNode != null) {
            setRangeMinimum(Double.parseDouble(rangeMinimumNode.getText()));
        }

    }

    /**
     * @param double rangeMinimum
     *        Set the minimum value of the range axis.
     */
    public void setRangeMinimum(double rangeMinimum) {
        this.rangeMinimum = rangeMinimum;
    }

    /**
     * Return the range axis' maximum value
     *
     * @return double Range axis' maximum value
     */
    public double getRangeMaximum() {
        return rangeMaximum;
    }

    public void setRangeMaximum(Node rangeMaximumNode) {
        if (rangeMaximumNode != null) {
            setRangeMaximum(Double.parseDouble(rangeMaximumNode.getText()));
        }

    }

    /**
     * @param double rangeMaximum
     *        Set the maximum value of the range axis.
     */
    public void setRangeMaximum(double rangeMaximum) {
        this.rangeMaximum = rangeMaximum;
    }

    /**
     * Return the domain axis' minimum value
     *
     * @return double domain axis' minimum value
     */
    public double getDomainMinimum() {
        return domainMinimum;
    }

    public void setDomainMinimum(Node domainMinimumNode) {
        if (domainMinimumNode != null) {
            setDomainMinimum(Double.parseDouble(domainMinimumNode.getText()));
        }

    }

    /**
     * @param double domainMinimum
     *        Set the minimum value of the domain axis.
     */
    public void setDomainMinimum(double domainMinimum) {
        this.domainMinimum = domainMinimum;
    }

    /**
     * Return the domain axis' maximum value
     *
     * @return double domain axis' maximum value
     */
    public double getDomainMaximum() {
        return domainMaximum;
    }

    public void setDomainMaximum(Node domainMaximumNode) {
        if (domainMaximumNode != null) {
            setDomainMaximum(Double.parseDouble(domainMaximumNode.getText()));
        }

    }

    /**
     * @param double domainMaximum
     *        Set the maximum value of the domain axis.
     */
    public void setDomainMaximum(double domainMaximum) {
        this.domainMaximum = domainMaximum;
    }

    public String getNoDataMessage() {
        return noDataMessage;
    }

    public String getTooltipContent() {
        return tooltipContent;
    }

    public void setTooltipContent(String tooltipContent) {
        this.tooltipContent = tooltipContent;
    }

    public String getTooltipXFormat() {
        return tooltipXFormat;
    }

    public void setTooltipXFormat(String tooltipXFormat) {
        this.tooltipXFormat = tooltipXFormat;
    }

    public String getTooltipYFormat() {
        return tooltipYFormat;
    }

    public void setTooltipYFormat(String tooltipYFormat) {
        this.tooltipYFormat = tooltipYFormat;
    }

    public void setTooltipContent(Node node) {
        if (node != null) {
            setTooltipContent(node.getText());
        }
    }

    public void setTooltipXFormat(Node node) {
        if (node != null) {
            setTooltipXFormat(node.getText());
        }
    }

    public void setTooltipYFormat(Node node) {
        if (node != null) {
            setTooltipYFormat(node.getText());
        }
    }

    public Float getBackgroundAlpha() {
        return backgroundAlpha;
    }

    public void setBackgroundAlpha(Node backgroundAlphaNode) {
        if (backgroundAlphaNode != null) {
            Float backgroundAlphaValue = new Float(backgroundAlphaNode.getText());
            this.backgroundAlpha = backgroundAlphaValue;
        }

    }

    public Float getForegroundAlpha() {
        return foregroundAlpha;
    }

    public void setForegroundAlpha(Node foregroundAlphaNode) {
        if (foregroundAlphaNode != null) {
            Float foregroundAlphaValue = new Float(foregroundAlphaNode.getText());
            this.foregroundAlpha = foregroundAlphaValue;
        }

    }
}