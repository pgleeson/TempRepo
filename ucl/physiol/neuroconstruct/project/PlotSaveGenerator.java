/**
 * neuroConstruct
 *
 * Software for developing large scale 3D networks of biologically realistic neurons
 * Copyright (c) 2007 Padraig Gleeson
 * UCL Department of Physiology
 *
 * Development of this software was made possible with funding from the
 * Medical Research Council
 *
 */

package ucl.physiol.neuroconstruct.project;

import java.util.*;

import ucl.physiol.neuroconstruct.utils.*;
import ucl.physiol.neuroconstruct.project.GeneratedPlotSaves.PlotSaveDetails;
import ucl.physiol.neuroconstruct.cell.Cell;
import ucl.physiol.neuroconstruct.cell.Segment;

/**
 * Thread to handle generation of what to plot/save during simulation run
 *
 * @author Padraig Gleeson
 * @version 1.0.4
 */


public class PlotSaveGenerator extends Thread
{
    ClassLogger logger = new ClassLogger("PlotSaveGenerator");

    public final static String myGeneratorType = "PlotSaveGenerator";

    Project project = null;
    long startGenerationTime;
    boolean continueGeneration = true;

    GenerationReport myReportInterface = null;

    private SimConfig simConfig = null;


    public PlotSaveGenerator(Project project, GenerationReport reportInterface)
    {
        super(myGeneratorType);

        logger.logComment("New PlotSaveGenerator created");
        this.project = project;

        myReportInterface = reportInterface;

    }

    public void setSimConfig(SimConfig simConfig)
    {
        this.simConfig = simConfig;
    }



    public void stopGeneration()
    {
        logger.logComment("PlotSaveGenerator being told to stop...");
        continueGeneration = false;
    }



    public void run()
    {
        logger.logComment("Running PlotSaveGenerator thread...");

        startGenerationTime = System.currentTimeMillis();

        project.generatedPlotSaves.reset();

        ArrayList<String> simPlotsInSimConfig = simConfig.getPlots();

        ArrayList<String> cellGroupsInSimConfig = simConfig.getCellGroups();

        for (String simPlotName: simPlotsInSimConfig)
        {
            SimPlot simPlot = project.simPlotInfo.getSimPlot(simPlotName);

            if (cellGroupsInSimConfig.contains(simPlot.getCellGroup()))
            {

                this.myReportInterface.giveUpdate("Generating: " + simPlot+"...");

                boolean allCellsInGroup = false;
                boolean allSegments = false;

                Cell nextCell = project.cellManager.getCell(project.cellGroupsInfo.getCellType(simPlot.getCellGroup()));

                ArrayList<Integer> cellNumsToPlot = new ArrayList<Integer>();
                ArrayList<Integer> segIdsToPlot = new ArrayList<Integer>();

                int numInCellGroup = project.generatedCellPositions.getNumberInCellGroup(simPlot.getCellGroup());


                /* Generate which cells in group to plot/save */

                if (simPlot.getCellNumber().equals("*"))
                {
                    allCellsInGroup = true;

                    for (int i = 0; i < numInCellGroup; i++)
                    {
                        cellNumsToPlot.add(new Integer(i));
                    }

                }
                else if (simPlot.getCellNumber().indexOf("%")>0)
                {
                    float percentage = -1;


                    try
                    {
                        percentage
                            = Float.parseFloat(simPlot.getCellNumber().substring(0,
                                                                                 simPlot.getCellNumber().length() - 1));
                    }
                    catch (NumberFormatException ex)
                    {
                        logger.logError("Badly formatted percentage. Using 0%", ex);

                    }

                    if (percentage > 100) percentage = 100;

                    if (percentage > 0)
                    {
                        int numToPlot = (int) Math.floor( ( ( (float) numInCellGroup * percentage) / 100) + 0.5);
                        if (numToPlot == 0) numToPlot = 1;

                        int numAlreadyUsed = 0;
                        while (numAlreadyUsed < numToPlot)
                        {
                            int nextCellNum = ProjectManager.getRandomGenerator().nextInt(numInCellGroup);
                            if (!cellNumsToPlot.contains(new Integer(nextCellNum)))
                            {
                                cellNumsToPlot.add(new Integer(nextCellNum));
                                numAlreadyUsed++;
                            }
                        }
                    }
                }
                else
                {
                    cellNumsToPlot.add(new Integer(simPlot.getCellNumber()));
                }


                /* Generate which segments in group to plot/save */


                Vector<Segment> segments = nextCell.getExplicitlyModelledSegments();

                if (simPlot.getSegmentId().equals("*"))
                {
                    allSegments = true;

                    for (Segment seg: segments)
                    {
                        segIdsToPlot.add(new Integer(seg.getSegmentId()));

                    }
                }
                else
                {
                    segIdsToPlot.add(new Integer(simPlot.getSegmentId()));

                }


                project.generatedPlotSaves.addPlotSaveDetails(simPlot.getPlotReference(),
                                                              simPlot,
                                                              cellNumsToPlot,
                                                              segIdsToPlot,
                                                              allCellsInGroup,
                                                              allSegments);
            }
            else
            {
                logger.logError(simPlot +" is in Sim Config but cell group: "+simPlot.getCellGroup()+" isn't!!");
            }
        }




        sendGenerationReport(false);

    }

    private void sendGenerationReport(boolean interrupted)
    {
        StringBuffer generationReport = new StringBuffer();


        long generationTime = System.currentTimeMillis();

        float seconds = (float) (generationTime - startGenerationTime) / 1000f;

        generationReport.append("<center><b>Simulation plotting/saving:</b></center>");
        generationReport.append("Time taken to generate: " + seconds +
                                " seconds.<br>");

        if (interrupted)
            generationReport.append("<center><b>NOTE: Generation interrupted</b></center><br>");

        ArrayList<PlotSaveDetails> plotSaves = project.generatedPlotSaves.getAllPlotSaves();


        if (plotSaves.size() == 0)
        {
            generationReport.append("Nothing generated for plotting/saving<br>");

        }
        for (PlotSaveDetails plotSave: plotSaves)
        {

            generationReport.append("<b>" + plotSave.simPlot.getPlotReference() + "</b><br>");

            generationReport.append(plotSave.getDescription(false)+" "

                                    + "<br>");
        }

        if (myReportInterface!=null)
        {
            myReportInterface.giveGenerationReport(generationReport.toString(),
                                                   myGeneratorType,
                                                   simConfig);
        }
    }


}
