/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sc.player2016.logic;

import java.util.ArrayList;
import sc.player2016.logic.Jinx.FieldColor;

/**
 *
 * @author Jonas
 */
public class Evaluator {
    private static final float TURN_ADVANTAGE = 2;
    private static final float MAX_VALUE = 20;
    private static float factor; 
    private static Board board;
    private static float help;
    
    //is called once at beginning in Jinx.findMove()
    public static void setFactor() {
        factor = Jinx.jinxIsPlayingVertical?1:-1;
    }
    //is called once at beginning in Jinx.findMove()
    public static void setBoard(Board b) {
        board = b;
    }

    public static float evaluateBoardPosition(ArrayList<Graph> graphsByVert,
            ArrayList<Graph> graphsByHor, boolean isVertsMove, int pointsByJinx, int pointsByOpponent){
            //Evaluates the current state of the board.
            //The higher the result is, the better is the situation for jinx
            float result;
            
            result = (pointsByJinx - pointsByOpponent);//Jinx.weightPoints * (pointsByJinx - pointsByOpponent);
            
            //evaluate conflictzone (important especially in the beginning of the game,
            //not at the end, l. weigthOfPoints())
            //if points and conflictzone are equal, evaluate the situation better 
            //if the graph is more centered (in both dimensions)
            
            if(isVertsMove){
                result += (1-Jinx.weightPoints) * evaluateVertsConflictzones(graphsByVert, graphsByHor, true);
//                result += (1-Jinx.weightPoints) * (evaluateVertsConflictzones(graphsByVert, graphsByHor, true) + 
//                        evaluateHorsConflictzones(graphsByVert, graphsByHor, false)) / 2;
                
                //dimension centering: x: minY and maxY seperately; y: average of both
                result -= 0.01f * Math.abs(11.5f - graphsByVert.get(0).getMinYField().getX());
                result -= 0.01f * Math.abs(11.5f - graphsByVert.get(0).getMaxYField().getX());
                result -= 0.01f * Math.abs(11.5f - 
                        ((graphsByVert.get(0).getMinYField().getY() +
                        graphsByVert.get(0).getMaxYField().getY()) / 2));
            }else{
//                result += (1-Jinx.weightPoints) * (evaluateVertsConflictzones(graphsByVert, graphsByHor, false) +
//                        evaluateHorsConflictzones(graphsByVert, graphsByHor, true)) / 2;
                result += (1-Jinx.weightPoints) * evaluateHorsConflictzones(graphsByVert, graphsByHor, true);
                
                //dimension centering: x: average of minX and maxX ; y: minX and maxX seperately
                result -= 0.01f * Math.abs(11.5f - 
                        ((graphsByHor.get(0).getMinXField().getX() +
                        graphsByHor.get(0).getMaxXField().getX()) / 2));
                result -= 0.01f * Math.abs(11.5f - graphsByHor.get(0).getMinXField().getY());
                result -= 0.01f * Math.abs(11.5f - graphsByHor.get(0).getMaxXField().getY());
            }
            
            return result;
    }
    
    
    private static float a = 0;
    private static float mid = 18;
    private static float up = 27;
    private static int x;
    private static float result;
    public static float weigthOfPoints(int round){
        //returns number between 0.2 (round==1) and 1 (round >= up)
        x = round;
        if(x <= 1){
            result = 0;
            
        }else if(x <= mid){
            result =  x*x*x*x*x/(2*mid*mid*mid*mid*mid);
            
        }else if(x <= up){
            result = 1 - (up-x)*(up-x)/(2*(up-mid)*(up-mid));
        }else{
            result = 1;
        }
        return 0.2f + 0.6f*result;
    }

    public static float evaluateVertsConflictzones(ArrayList<Graph> graphsByVert,
            ArrayList<Graph> graphsByHor, boolean isVertsMove){
        //iterate through all graphs (with at least 2 points) by vert.
        //Each graph has a start point pS and an end point pE.
        //For each point a conflictzone is evaluated (4-geradenprinzip). 
        //This function returns the greatest minimum (lower conflictzone, 
        //either with pS or pE) of a graph by vert.
        
        //Greatest minimum of a graph
        float minConflictzone = -MAX_VALUE;
        float help;
        
        for(Graph g : graphsByVert){
            if(g.hasJustOneField()) break;//just graphs with at least 2 fields are relevant
            
            help = evaluateVertGraph(g, graphsByHor, isVertsMove);
//            System.out.println("Graph " + g + "\nconflict: " + help);
            minConflictzone = Math.max(minConflictzone, help);
        }
        minConflictzone = minConflictzone>=0?Math.min(minConflictzone, MAX_VALUE):
                -1 * Math.min(Math.abs(minConflictzone), MAX_VALUE);
        return factor * minConflictzone;
    }
    
    private static float evaluateVertGraph(Graph vertGraph, 
            ArrayList<Graph> graphsByHor, boolean isVertsMove){
        float minYConflict = MAX_VALUE;
        float maxYConflict = MAX_VALUE;
        Field fVMin = vertGraph.getMinYField();
        Field fVMax = vertGraph.getMaxYField();
        
        float help;
        Field fHMin;
        Field fHMax;
        
        for(Graph g : graphsByHor){
            if(g.hasJustOneField()) break;//just graphs with at least 2 fields are relevant
            fHMax = g.getMaxXField();
            fHMin = g.getMinXField();
            
            //fVMin
            if(fHMax.getX() <= fVMin.getX()){//g left from vertGraphMinY
                if(fHMax.getY() <= fVMin.getY()){//g up&left from vertGraphMinY

                    //conflict is in up&right corner
                    Field pV = new Field(23 - fVMin.getX(), fVMin.getY());
                    Field pH = new Field(23 - fHMax.getX(), fHMax.getY());
                    if(!isVertsMove){
                        //it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates)
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    minYConflict = Math.min(minYConflict, help);
                }
            }else if(fHMin.getX() >= fVMin.getX()){//g right from vertGraphMinY
                if(fHMin.getY() <= fVMin.getY()){//g up&right from vertGraphMinY

                    //conflict is in up&left corner
                    Field pV = fVMin;
                    Field pH = fHMin;
                    if(!isVertsMove){
                        //it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates)
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    minYConflict = Math.min(minYConflict, help);
                }
            }else{//g middle from vertGraphMinY
                if(gBlocksVertMin(fVMin, g)){//g up&middle from vertGraphMinY
                    
                    //check if conflictsituation could be won, although g blocks
                    if(fHMin.getY() < fVMin.getY() && 
                            fHMin.getX() + 1 == fVMin.getX()){
                        
                        //check if g blocks vertMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fHMin.getY() == fVMin.getY() - 1 &&
                                fHMin.isConnectedWith(fHMin.getX() + 1, fHMin.getY() + 2))){
                            
                            //conflict is in up&left corner
                           Field pV = fVMin;
                           Field pH = fHMin;
                           if(!isVertsMove){
                               //it is hors move, so swap pV and pH 
                               //(reflect them by swapping x and y coordinates)
                               Field f = pV;
                               pV = new Field(pH.getY(), pH.getX());
                               pH = new Field(f.getY(), f.getX());
                           }
                           help = evaluateConflictPoints(pV, pH);
                           minYConflict = Math.min(minYConflict, help);
                        }
                        
                    }else if(fHMax.getY() < fVMin.getY() && 
                            fHMax.getX() - 1 == fVMin.getX()){
                        
                        //check if g blocks vertMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fHMax.getY() == fVMin.getY() - 1 &&
                                fHMax.isConnectedWith(fHMax.getX() - 1, fHMax.getY() + 2))){
                            
                            //conflict is in up&right corner
                           Field pV = new Field(23 - fVMin.getX(), fVMin.getY());
                           Field pH = new Field(23 - fHMax.getX(), fHMax.getY());
                           if(!isVertsMove){
                               //it is hors move, so swap pV and pH 
                               //(reflect them by swapping x and y coordinates)
                               Field f = pV;
                               pV = new Field(pH.getY(), pH.getX());
                               pH = new Field(f.getY(), f.getX());
                           }
                           help = evaluateConflictPoints(pV, pH);
                           minYConflict = Math.min(minYConflict, help);
                        }
                    
                    }else{//usual case
                        minYConflict = -MAX_VALUE;
                    }
                }
            }
            
            //fVMax
            if(fHMax.getX() <= fVMax.getX()){//g left from vertGraphMaxY
                if(fHMax.getY() >= fVMax.getY()){//g down&left from vertGraphMaxY
                    
                    //conflict is in down&right corner
                    Field pV = new Field(23 - fVMax.getX(), 23 - fVMax.getY());
                    Field pH = new Field(23 - fHMax.getX(), 23 - fHMax.getY());
                    if(!isVertsMove){
                        //it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates)
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    maxYConflict = Math.min(maxYConflict, help);
                }
            }else if(fHMin.getX() >= fVMax.getX()){//g right from vertGraphMaxY
                if(fHMin.getY() >= fVMax.getY()){//g down&right from vertGraphMaxY

                    //conflict is in down&left corner
                    Field pV = new Field(fVMax.getX(), 23 - fVMax.getY());
                    Field pH = new Field(fHMin.getX(), 23 - fHMin.getY());
                    if(!isVertsMove){
                        //it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates)
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    maxYConflict = Math.min(maxYConflict, help);
                }
            }else{//g middle from vertGraphMinY
                if(gBlocksVertMax(fVMax, g)){//g down&middle from vertGraphMaxY
                    
                    //check if conflictsituation could be won, although g blocks
                    if(fHMin.getY() > fVMax.getY() && 
                            fHMin.getX() + 1 == fVMax.getX()){
                        
                        //check if g blocks vertMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fHMin.getY() == fVMax.getY() + 1 &&
                                fHMin.isConnectedWith(fHMin.getX() + 1, fHMin.getY() - 2))){
                
                            //conflict is in down&left corner
                            Field pV = new Field(fVMax.getX(), 23 - fVMax.getY());
                            Field pH = new Field(fHMin.getX(), 23 - fHMin.getY());
                            if(!isVertsMove){
                                //it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates)
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            maxYConflict = Math.min(maxYConflict, help);
                        }
                    }else if(fHMax.getY() > fVMax.getY() && 
                            fHMax.getX() - 1 == fVMax.getX()){
                        
                        //check if g blocks vertMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fHMax.getY() == fVMax.getY() + 1 &&
                                fHMax.isConnectedWith(fHMax.getX() - 1, fHMax.getY() - 2))){
                            
                            //conflict is in down&right corner
                            Field pV = new Field(23 - fVMax.getX(), 23 - fVMax.getY());
                            Field pH = new Field(23 - fHMax.getX(), 23 - fHMax.getY());
                            if(!isVertsMove){
                                //it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates)
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            maxYConflict = Math.min(maxYConflict, help);
                        }
                    
                    }else{//usual case
                        maxYConflict = -MAX_VALUE;
                    }
                }
            }
        }
        return Math.min(minYConflict, maxYConflict);
    }
    
    public static float evaluateHorsConflictzones(ArrayList<Graph> graphsByVert,
            ArrayList<Graph> graphsByHor, boolean isHorsMove){
        //iterate through all graphs (with at least 2 points) by hor.
        //Each graph has a start point pS and an end point pE.
        //For each point a conflictzone is evaluated (4-geradenprinzip). 
        //This function returns the greatest minimum (lower conflictzone, 
        //either with pS or pE) of a graph by hor.
        
        //Greatest minimum of a graph
        float minConflictzone = -MAX_VALUE;
        float help;
        
        for(Graph g : graphsByHor){
            if(g.hasJustOneField()) break;//just graphs with at least 2 fields are relevant
            
            help = evaluateHorGraph(g, graphsByVert, isHorsMove);
//            System.out.println("Graph " + g + "\nconflict: " + help);
            minConflictzone = Math.max(minConflictzone, help);
        }
        minConflictzone = minConflictzone>=0?Math.min(Math.abs(minConflictzone), MAX_VALUE):
                -1 * Math.min(Math.abs(minConflictzone), MAX_VALUE);
        return -1 * factor * minConflictzone;
    }

    private static float evaluateHorGraph(Graph horGraph, 
            ArrayList<Graph> graphsByVert, boolean isHorsMove){
        float minXConflict = MAX_VALUE;
        float maxXConflict = MAX_VALUE;
        Field fHMin = horGraph.getMinXField();
        Field fHMax = horGraph.getMaxXField();
        
        float help;
        Field fVMin;
        Field fVMax;
        
        for(Graph g : graphsByVert){
            if(g.hasJustOneField()) break;//just graphs with at least 2 fields are relevant
            fVMax = g.getMaxYField();
            fVMin = g.getMinYField();
            
            //fHMin
            if(fVMax.getY() <= fHMin.getY()){//g up from horGraphMinX
                if(fVMax.getX() <= fHMin.getX()){//g up&left from horGraphMinX
                    
                    //conflict is in down&left corner 
                    Field pV = new Field(fVMax.getX(), 23 - fVMax.getY());
                    Field pH = new Field(fHMin.getX(), 23 - fHMin.getY());
                    if(isHorsMove){
                        //(it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates))
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    minXConflict = Math.min(minXConflict, help);
                }
            }else if(fVMin.getY() >= fHMin.getY()){//g down from horGraphMinX
                if(fVMin.getX() <= fHMin.getX()){//g down&left from horGraphMinX
//                    help = (fVMin.getY() - fHMin.getY()) - 
//                            (fHMin.getX() - fVMin.getX());// + TURN_ADVANTAGE
                    //conflict is in up&left corner
                    Field pV = fVMin;
                    Field pH = fHMin; 
                    if(isHorsMove){
                        //(it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates))
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    minXConflict = Math.min(minXConflict, help);
                }
            }else{//g middle from horGraphMinX
                if(gBlocksHorMin(fHMin, g)){// g left&middle from horGraphMinX
                    
                    //check if conflictsituation could be won, although g blocks
                    if(fVMin.getX() < fHMin.getX() && 
                            fVMin.getY() + 1 == fHMin.getY()){
                        
                        //check if g blocks horMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fVMin.getX() == fHMin.getX() - 1 &&
                                fVMin.isConnectedWith(fVMin.getX() + 2, fVMin.getY() + 1))){
                            
                            //conflict is in up&left corner
                            Field pV = fVMin;
                            Field pH = fHMin;
                            if(isHorsMove){
                                //(it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates))
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            minXConflict = Math.min(minXConflict, help); 
                        }
                        
                    }else if(fVMax.getX() < fHMin.getX() && 
                            fVMax.getY() - 1 == fHMin.getY()){
                        
                        //check if g blocks horMin in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fVMax.getX() == fHMin.getX() - 1 &&
                             fVMax.isConnectedWith(fVMax.getX() + 2, fVMax.getY() - 1))){
                            
                            //conflict is in down&left corner
                            Field pV = new Field(fVMax.getX(), 23 - fVMax.getY());
                            Field pH = new Field(fHMin.getX(), 23 - fHMin.getY());
                            if(isHorsMove){
                                //(it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates))
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            minXConflict = Math.min(minXConflict, help);
                        }
                        
                    
                    }else{//usual case
                        minXConflict = -MAX_VALUE;
                    }
                }
            }
            
            //fHMax
            if(fVMax.getY() <= fHMax.getY()){//g up from horGraphMaxX
                if(fVMax.getX() >= fHMax.getX()){//g up&right from horGraphMaxX
                    
                    //conflict is in down&right corner
                    Field pV = new Field(23 - fVMax.getX(), 23 - fVMax.getY());
                    Field pH = new Field(23 - fHMax.getX(), 23 - fHMax.getY());
                    if(isHorsMove){
                        //(it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates))
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    maxXConflict = Math.min(maxXConflict, help);
                }
            }else if(fVMin.getY() >= fHMax.getY()){//g down from horGraphMaxX
                if(fVMin.getX() >= fHMax.getX()){//g down&right from horGraphMaxX

                    //conflict is in up&right corner
                    Field pV = new Field(23 - fVMin.getX(), fVMin.getY());
                    Field pH = new Field(23 - fHMax.getX(), fHMax.getY());
                    if(isHorsMove){
                        //(it is hors move, so swap pV and pH 
                        //(reflect them by swapping x and y coordinates))
                        Field f = pV;
                        pV = new Field(pH.getY(), pH.getX());
                        pH = new Field(f.getY(), f.getX());
                    }
                    help = evaluateConflictPoints(pV, pH);
                    maxXConflict = Math.min(maxXConflict, help);
                }
            }else{//g middle from horGraphMaxX
                if(gBlocksHorMax(fHMax, g)){// g right&middle from horGraphMaxX
                    
                    //check if conflictsituation could be won, although g blocks
                    if(fVMin.getX() > fHMax.getX() && 
                            fVMin.getY() + 1 == fHMax.getY()){
                        
                        //check if g blocks horMax in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fVMin.getX() == fHMax.getX() + 1 &&
                             fVMin.isConnectedWith(fVMin.getX() - 2, fVMin.getY() + 1))){
                            
                            //conflict is in up&right corner
                            Field pV = new Field(23 - fVMin.getX(), fVMin.getY());
                            Field pH = new Field(23 - fHMax.getX(), fHMax.getY());
                            if(isHorsMove){
                                //(it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates))
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            maxXConflict = Math.min(maxXConflict, help);
                        }
                       
                        
                    }else if(fVMax.getX() > fHMax.getX() && 
                            fVMax.getY() - 1 == fHMax.getY()){
                        
                                                //check if g blocks horMax in the middle of the graph 
                        //(depth 6 problem; jinx waited for opponent to attack)
                        if(!(fVMax.getX() == fHMax.getX() + 1 &&
                             fVMax.isConnectedWith(fVMax.getX() - 2, fVMax.getY() - 1))){
                            
                            //conflict is in down&right corner
                            Field pV = new Field(23 - fVMax.getX(), 23 - fVMax.getY());
                            Field pH = new Field(23 - fHMax.getX(), 23 - fHMax.getY());
                            if(isHorsMove){
                                //(it is hors move, so swap pV and pH 
                                //(reflect them by swapping x and y coordinates))
                                Field f = pV;
                                pV = new Field(pH.getY(), pH.getX());
                                pH = new Field(f.getY(), f.getX());
                            }
                            help = evaluateConflictPoints(pV, pH);
                            maxXConflict = Math.min(maxXConflict, help);
                        }
                    
                    }else{//usual case
                        minXConflict = -MAX_VALUE;
                    }
                }
            }
        }
        return Math.min(minXConflict, maxXConflict);
    }
    
    private static boolean gBlocksHorMax(Field horMax, Graph vertGraph){
//        System.out.println("horMax: " + horMax + " vertGraph = " + vertGraph);
        int y = horMax.getY();
        FieldColor vertColor = Jinx.jinxIsPlayingVertical?
                Jinx.FieldColor.JINX:Jinx.FieldColor.OPPONENT;
        //iterate over two lines right from HorGraphMaxX
        for(int x=horMax.getX(); x <= vertGraph.getMaxXField().getX(); x++){
            //either vertGraph blocks with field (x,y)
            if(board.getField(x, y).getFieldColor() == vertColor &&
                (board.getFieldSave(x-2, y-1).getFieldColor() == vertColor ||
                 board.getFieldSave(x-1, y-2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+1, y-2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+2, y-1).getFieldColor() == vertColor) && 
                (board.getFieldSave(x-2, y+1).getFieldColor() == vertColor ||
                 board.getFieldSave(x-1, y+2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+1, y+2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+2, y+1).getFieldColor() == vertColor)){
//                System.out.println("true 1");
                return true;
            }
            
            //or it blocks with ( , y-1) connected with ( , y+1)
            if((board.getFieldSave(x-1, y-1).getFieldColor() == vertColor &&
                board.getFieldSave(x, y+1).getFieldColor() == vertColor) ||
               (board.getFieldSave(x, y-1).getFieldColor() == vertColor &&
                board.getFieldSave(x-1, y+1).getFieldColor() == vertColor)){
//                System.out.println("true 2");
                return true;
            }
        }
//        System.out.println("false");
        return false;
    }
    
    private static boolean gBlocksHorMin(Field horMin, Graph vertGraph){
//        System.out.println("horMin: " + horMin + " vertGraph = " + vertGraph);
        int y = horMin.getY();
        FieldColor vertColor = Jinx.jinxIsPlayingVertical?
                Jinx.FieldColor.JINX:Jinx.FieldColor.OPPONENT;
        
        //iterate over two lines left from HorGraphMinX
        for(int x=horMin.getX(); x >= vertGraph.getMinXField().getX(); x--){
            
            //either vertGraph blocks with field (x,y)
            if(board.getField(x, y).getFieldColor() == vertColor &&
                (board.getFieldSave(x-2, y-1).getFieldColor() == vertColor ||
                 board.getFieldSave(x-1, y-2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+1, y-2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+2, y-1).getFieldColor() == vertColor) && 
                (board.getFieldSave(x-2, y+1).getFieldColor() == vertColor ||
                 board.getFieldSave(x-1, y+2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+1, y+2).getFieldColor() == vertColor ||
                 board.getFieldSave(x+2, y+1).getFieldColor() == vertColor)){
//                System.out.println("true 1");
                return true;
            }
            
            //or it blocks with ( , y-1) connected with ( , y+1)
            if((board.getFieldSave(x+1, y-1).getFieldColor() == vertColor &&
                board.getFieldSave(x, y+1).getFieldColor() == vertColor) ||
               (board.getFieldSave(x, y-1).getFieldColor() == vertColor &&
                board.getFieldSave(x+1, y+1).getFieldColor() == vertColor)){
//                System.out.println("true 2");
                return true;
            }
        }
//        System.out.println("false");
        return false;
    }
    
    private static boolean gBlocksVertMax(Field vertMax, Graph horGraph){
        int x = vertMax.getX();
        FieldColor horColor = Jinx.jinxIsPlayingVertical?
                Jinx.FieldColor.OPPONENT:Jinx.FieldColor.JINX;
        
        //iterate over two columns down from VertGraphMaxY
        for(int y=vertMax.getY(); y <= horGraph.getMaxYField().getY(); y++){
            //either horGraph blocks with field (x,y)
            if(board.getField(x, y).getFieldColor() == horColor &&
                (board.getFieldSave(x-1, y-2).getFieldColor() == horColor ||
                 board.getFieldSave(x-2, y-1).getFieldColor() == horColor ||
                 board.getFieldSave(x-2, y+1).getFieldColor() == horColor ||
                 board.getFieldSave(x-1, y+2).getFieldColor() == horColor) && 
                (board.getFieldSave(x+1, y-2).getFieldColor() == horColor ||
                 board.getFieldSave(x+2, y-1).getFieldColor() == horColor ||
                 board.getFieldSave(x+2, y+1).getFieldColor() == horColor ||
                 board.getFieldSave(x+1, y+2).getFieldColor() == horColor)){
                return true;
            }
            
            //or it blocks with (x-1 , ) connected with (x+1 ,  )
            if((board.getFieldSave(x-1, y-1).getFieldColor() == horColor &&
                board.getFieldSave(x+1, y).getFieldColor() == horColor) ||
               (board.getFieldSave(x-1, y).getFieldColor() == horColor &&
                board.getFieldSave(x+1, y-1).getFieldColor() == horColor)){
                return true;
            }
        }
        return false;
    }
    
    private static boolean gBlocksVertMin(Field vertMin, Graph horGraph){
        int x = vertMin.getX();
        FieldColor horColor = Jinx.jinxIsPlayingVertical?
                Jinx.FieldColor.OPPONENT:Jinx.FieldColor.JINX;
        
        //iterate over two columns down from VertGraphMaxY
        for(int y=vertMin.getY(); y >= horGraph.getMinYField().getY(); y--){
            //either horGraph blocks with field (x,y)
            if(board.getField(x, y).getFieldColor() == horColor &&
                (board.getFieldSave(x-1, y-2).getFieldColor() == horColor ||
                 board.getFieldSave(x-2, y-1).getFieldColor() == horColor ||
                 board.getFieldSave(x-2, y+1).getFieldColor() == horColor ||
                 board.getFieldSave(x-1, y+2).getFieldColor() == horColor) && 
                (board.getFieldSave(x+1, y-2).getFieldColor() == horColor ||
                 board.getFieldSave(x+2, y-1).getFieldColor() == horColor ||
                 board.getFieldSave(x+2, y+1).getFieldColor() == horColor ||
                 board.getFieldSave(x+1, y+2).getFieldColor() == horColor)){
                return true;
            }
            
            //or it blocks with (x-1 , ) connected with (x+1 ,  )
            if((board.getFieldSave(x-1, y+1).getFieldColor() == horColor &&
                board.getFieldSave(x+1, y).getFieldColor() == horColor) ||
               (board.getFieldSave(x-1, y).getFieldColor() == horColor &&
                board.getFieldSave(x+1, y+1).getFieldColor() == horColor)){
                return true;
            }
        }
        return false;
    }
    
    private static float evaluateConflictPoints(Field pV, Field pH){
        //It is always verts move. (If it was actually hors move, the arguments 
        //were just reflected/swapped).
        //The conflict is always in the down-left corner (per reflection).
        //pH.getY() is always smaller than pV.getY() (down-left corner is coordinate origin now!).
        //This function finds out the best lines (1, 2, 3 or 4) for each point,
        //calculates the intersection points with the axises (vert line with X-axis,
        //hor line with y-axis) and returns a result based on these.
        int minNotBeatableLineByVert = -1;
        int minNotBeatableLineByHor = -1;
        
        for(int i=0; i<4; i++){
            if(findBeaterLineFor(i, true, pV, pH) == -1){
                minNotBeatableLineByVert = i;
                break;
            }
        }
        
        for(int i=0; i<4; i++){
            if(findBeaterLineFor(i, false, pV, pH) == -1){
                minNotBeatableLineByHor = i;
                break;
            }
        }
        
        if(minNotBeatableLineByHor != -1 && minNotBeatableLineByVert != -1){
            //both players have a line, thopponent cannot beat (that is the common case)
            //the found best lines are parallel
            float pVert = calcPointsWithLine(minNotBeatableLineByVert, pV, true);
//            System.out.println("pPlayer = " + pPlayer);
            
            float pHor = calcPointsWithLine(minNotBeatableLineByHor, pH, false);
//            System.out.println("pOpponent = " + pOpponent);
            return (Math.max(pVert, 0) - Math.max(pHor, 0));
        }else if(minNotBeatableLineByVert != -1){
            //only the current player has a line the opponent cannot beat
            float pVert = calcPointsWithLine(minNotBeatableLineByVert, pV, true);
//            System.out.println("pPlayer = " + pPlayer);
            return pVert;
            
        }else if(minNotBeatableLineByHor != -1){
            //only the other player has a line the opponent cannot beat
            float pHor = calcPointsWithLine(minNotBeatableLineByHor, pH, false);
            return -1 * pHor;
            
        }else{
            //should not be possible
            assert(false);
            return 0;
        }
    }
    
    private static Field pV;//vertical (playing) point of conflict zone (translated to the down-left corner equivalent)
    private static Field pH;//horizontal (playing) point of conflict zone (translated to the down-left corner equivalent)
    static float evaluateCurrentConflictzone(ArrayList<Graph> graphsByVert,
            ArrayList<Graph> graphsByHor, boolean isVertsMove){
        //find conflictzone and set the 2 relevant points
        //(reduce every conflictzone to a conflict in the down-left corner:
        //vertical player tries to reach the bottom and horizontal tries 
        //to reach the left border. This is necessary to use the 'hand-calculated'
        //formulas for the coordinate system; down-left corner of the board is 
        //defined as coordinate origin (0|0))
        if(Jinx.jinxIsPlayingVertical){
            if(graphsByVert.get(0).hasJustOneField())
                return Integer.MIN_VALUE;
        }else{
            if(graphsByHor.get(0).hasJustOneField())
                return Integer.MIN_VALUE;
        }
        calcPVAndPHFromAllGraphs(graphsByVert, graphsByHor);
 
//        System.out.println("pV = " + pV + " pH = " + pH);
        
        //IT IS ALWAYS VERTS MOVE, THAT MAKES THINGS A LOT EASIER
        if(!isVertsMove){
            Field f = pV;
            pV = new Field(pH.getY(), pH.getX());
            pH = new Field(f.getY(), f.getX());
        }
        //now it is verts move ;)   
        
//        System.out.println("pV = " + pV + " and pH = " + pH);
        
        
        
//        System.out.println("Vert is faster: " +
//                verticalLineIsFaster(0.5f, new Field(5,8), 
//                        2f, new Field(7,2), true));
        /*
        There are four possible lines for each player:
        v0(x) = -0.5 * x + 0.5 * xV + yV
        v1(x) = -2   * x + 2   * xV + yV
        v2(x) = 2    * x - 2   * xV + yV
        v3(x) = 0.5  * x - 0.5 * xV + yV
        for the vertical player (v0 would be best, v3 worst) and
        
        h0(x) = -2   * x + 2   * xH + yH
        h1(x) = -0.5 * x + 0.5 * xH + yH
        h2(x) =  0.5 * x - 0.5 * xH + yH
        h3(x) =  2   * x - 2   * xH + yH
        for the horizontal player (h0 would be best, h3 worst).
        
        This function now figures out which is the best line for each player
        that the opponent cannot beat. There can be 3 different results:
        1) Both players have a line that cannot be beaten by the other player
           (Common case, parallel or intersection point out of relevant area)
        2) Current player has a line that beats all opponent lines
        3) Other player has a line that beats all lines of current player
        
        Now the points (distance from intersection point of line with border 
        (x-axis for vertical player, y-axis for horizontal player) to (0|0))
        are calculated for the found lines.
        */
        int factor = isVertsMove==Jinx.jinxIsPlayingVertical?1:-1; //for return statements
        int minNotBeatableLineByOpponent = -1;
        int minNotBeatableLineByPlayer = -1;
        
        //find minNotBeatableLineByPlayer (is always playing vertical, look above)
        for(int i=0; i<4; i++){
//            System.out.println("i = " + i);
            if(findBeaterLineFor(i, true, pV, pH) == -1){
                minNotBeatableLineByPlayer = i;
//                System.out.println("Not beatable line = " + i);
                break;
            }
        }
        
        //find minNotBeatableLineByOpponent (is playing horizontal)
        for(int i=0; i<4; i++){
//            System.out.println("i = " + i);
            if(findBeaterLineFor(i, false, pV, pH) == -1){
                minNotBeatableLineByOpponent = i;
//                System.out.println("Not beatable line opponent = " + i);
                break;
            }
        }
        
        //TODO: Improve, rethink!
        if(minNotBeatableLineByOpponent != -1 && minNotBeatableLineByPlayer != -1){
            //both players have a line, thopponent cannot beat (that is the common case)
            //the found best lines are parallel
            float pPlayer = calcPointsWithLine(minNotBeatableLineByPlayer, pV, true);
//            System.out.println("pPlayer = " + pPlayer);
            
            float pOpponent = calcPointsWithLine(minNotBeatableLineByOpponent, pH, false);
//            System.out.println("pOpponent = " + pOpponent);
            return factor * (pPlayer - pOpponent);
        }else if(minNotBeatableLineByPlayer != -1){
            //only the current player has a line the opponent cannot beat
            float pPlayer = calcPointsWithLine(minNotBeatableLineByPlayer, pV, true);
//            System.out.println("pPlayer = " + pPlayer);
            return factor * pPlayer;
            
        }else if(minNotBeatableLineByOpponent != -1){
            //only the other player has a line the opponent cannot beat
            float pOpponent = calcPointsWithLine(minNotBeatableLineByOpponent, pH, false);
//            System.out.println("pOpponent = " + pOpponent);
            return factor * -1 * pOpponent;
            
        }else{
            
            //should not be possible
            assert(false);
            return 0;
        }
    }
    
    private static void calcPVAndPH(Field startOfJinxGraph, Field endOfJinxGraph,
            Field startOfOpponentGraph, Field endOfOpponentGraph){
        //helper vars for setting pV and pH
        float minSquaredDistance;
        float help;
        
        
        
        //the translation to pV and pH is tricky because of two aspects:
        //1. translate in another corner (down-left)
        //2. translate in another coordinate system (origin is in down-left corner now
        //   -> (0|23) transforms to (0|0))
        //That is why e. g. when the conflictzone is in the up-left corner,
        //we can just copy the points and have the right points in the down-left 
        //corner in the new coordinate system
        if(Jinx.jinxIsPlayingVertical){
            //set first to startJinx to startOpponent (conflictzone: up-left)
            minSquaredDistance = getSquaredDistance(startOfJinxGraph, startOfOpponentGraph);
            pV = startOfJinxGraph;
            pH = startOfOpponentGraph;
//            System.out.println("vert up-left");
            
//  ---------------- TODO! ---------------------
//            if(startOfJinxGraph == endOfJinxGraph || startOfOpponentGraph == endOfOpponentGraph)
            
            //check startJinx to endOpponent (conflictzone: up-right)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfJinxGraph.getX(),    startOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(),  endOfOpponentGraph.getY());
//                System.out.println("vert up-right");
            }

            //check endJinx to startOpponent (conflictzone: down-left)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfJinxGraph.getX(),       23 - endOfJinxGraph.getY());
                pH = new Field(startOfOpponentGraph.getX(), 23 - startOfOpponentGraph.getY());
//                System.out.println("vert down-left");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfJinxGraph.getX(),     23 - endOfJinxGraph.getY());
                pH = new Field(23 - endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
//                System.out.println("vert down-right");
            }
            
            
        }else{//jinx is playing horizontal
            //set first to startJinx to startOpponent (conflictzone: up-left)
            minSquaredDistance = getSquaredDistance(startOfJinxGraph, startOfOpponentGraph);
            pV = startOfOpponentGraph;
            pH = startOfJinxGraph;
//            System.out.println("hor up-left");
        
            //check startJinx to endOpponent (conflictzone: down-left)
            help = getSquaredDistance(startOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(endOfOpponentGraph.getX(), 23 - endOfOpponentGraph.getY());
                pH = new Field(startOfJinxGraph.getX(),   23 - startOfJinxGraph.getY());
//                System.out.println("hor down-left");
                
            }

            //check endJinx to startOpponent (conflictzone: up-right)
            help = getSquaredDistance(endOfJinxGraph, startOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - startOfOpponentGraph.getX(), startOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),       endOfJinxGraph.getY());
//                System.out.println("hor up-right");
            }

            //check endJinx to endOpponent (conflictzone: down-right)
            help = getSquaredDistance(endOfJinxGraph, endOfOpponentGraph);
            if(help < minSquaredDistance){
                minSquaredDistance = help;
                pV = new Field(23 - endOfOpponentGraph.getX(),  23 - endOfOpponentGraph.getY());
                pH = new Field(23 - endOfJinxGraph.getX(),      23 - endOfJinxGraph.getY());
//                System.out.println("hor down-right");
            }
        }

//        System.out.println("pV: " + pV + " pH: " + pH);
        
    }
    
    private enum Corner{
        MIN_MIN, MIN_MAX, MAX_MIN, MAX_MAX
    }
    
    private static void calcPVAndPHFromAllGraphs(ArrayList<Graph> graphsByVert, 
            ArrayList<Graph> graphsByHor){
        //check all graphs that have at least 2 fields (= more than 0 points)
        float minSquaredDistance = Float.MAX_VALUE;
        Field pVert = null;//to make netbeans happy
        Field pHor = null;//to make netbeans happy
        float help;
        Corner cornerOfMinDistancePoints = null;
        
        //set to default values (that result in an evaluation of currentConflictzone = 0),
        //if one player has no graph, that is worth more than 0 points
        pV = new Field(1,0);
        pH = new Field(0,1);
        
        //calc min distance fields (=Conflictzone); just search in start/end fields of graphs
        for(Graph gV : graphsByVert){
            if(gV.hasJustOneField()) break;//graphs are sorted, so all graphs with
                                            //points > 0 (2 or more fields) are already searched
            for(Graph gH : graphsByHor){
                if(gH.hasJustOneField()) break;//graphs are sorted, so all graphs with
                                                //points > 0 (2 or more fields) are already searched
                
                if(gV.getMinYField().getY() > 1 && gH.getMinXField().getX() > 1){
                    help = getSquaredDistance(gV.getMinYField(), gH.getMinXField());
                    if(help < minSquaredDistance){
                        minSquaredDistance = help;
                        cornerOfMinDistancePoints = Corner.MIN_MIN;
                        pVert = gV.getMinYField();
                        pHor = gH.getMinXField();
                    }
                }
                if(gV.getMinYField().getY() > 1 && gH.getMaxXField().getX() < 22){
                    help = getSquaredDistance(gV.getMinYField(), gH.getMaxXField());
                    if(help < minSquaredDistance){
                        minSquaredDistance = help;
                        cornerOfMinDistancePoints = Corner.MIN_MAX;
                        pVert = gV.getMinYField();
                        pHor = gH.getMaxXField();
                    }
                }
                if(gV.getMaxYField().getY() < 22 && gH.getMinXField().getX() > 1){
                    help = getSquaredDistance(gV.getMaxYField(), gH.getMinXField());
                    if(help < minSquaredDistance){
                        minSquaredDistance = help;
                        cornerOfMinDistancePoints = Corner.MAX_MIN;
                        pVert = gV.getMaxYField();
                        pHor = gH.getMinXField();
                    }
                }
                if(gV.getMaxYField().getY() < 22 && gH.getMaxXField().getX() < 22){
                    help = getSquaredDistance(gV.getMaxYField(), gH.getMaxXField());
                    if(help < minSquaredDistance){
                        minSquaredDistance = help;
                        cornerOfMinDistancePoints = Corner.MAX_MAX;
                        pVert = gV.getMaxYField();
                        pHor = gH.getMaxXField();
                    }
                }
            }
        }
        
        //translate in down-left equivalent (also change coordinate system:
        //current (0|23) becomes (0|0))
        if(cornerOfMinDistancePoints == Corner.MIN_MIN){//up-left corner
            pV = pVert;
            pH = pHor;
        }else if(cornerOfMinDistancePoints == Corner.MIN_MAX){//up-right
            pV = new Field(23 - pVert.getX(), pVert.getY());
            pH = new Field(23 - pHor.getX(), pHor.getY());
            
        }else if(cornerOfMinDistancePoints == Corner.MAX_MIN){//down-left
            pV = new Field(pVert.getX(), 23 - pVert.getY());
            pH = new Field(pHor.getX(), 23 - pHor.getY());
            
        }else if(cornerOfMinDistancePoints == Corner.MAX_MAX){//down-right
            pV = new Field(23 - pVert.getX(), 23 - pVert.getY());
            pH = new Field(23 - pHor.getX(), 23 - pHor.getY());
        }
    }
            
    private static final float[] mV = {
        -0.5f, -2, 2, 0.5f
    };
    private static final float[] mH = {
        -2, -0.5f, 0.5f, 2
    };
    
    private static int findBeaterLineFor(int i, boolean isVertLine, Field pV, Field pH){
        /*
        assert(i>=0 && i<4)
        There are four possible lines for each player:
        v0(x) = -0.5 * x + 0.5 * xV + yV
        v1(x) = -2   * x + 2   * xV + yV
        v2(x) = 2    * x - 2   * xV + yV
        v3(x) = 0.5  * x - 0.5 * xV + yV
        for the vertical player (v0 would be best, v3 worst) and
        
        h0(x) = -2   * x + 2   * xH + yH
        h1(x) = -0.5 * x + 0.5 * xH + yH
        h2(x) =  0.5 * x - 0.5 * xH + yH
        h3(x) =  2   * x - 2   * xH + yH
        for the horizontal player (h0 would be best, h3 worst).
        */
        
        //TODO: parallel lines
        
        if(isVertLine){
//            System.out.println("player tries against 0");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[0], pH))
                return 0;
//            System.out.println("player tries against 1");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[1], pH))
                return 1;
//            System.out.println("player tries against 2");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[2], pH))
                return 2;
//            System.out.println("player tries against 3");
            if(-1 == verticalLineIsFaster(mV[i], pV, mH[3], pH))
                return 3;
//            System.out.println("player beats opponent");
            return -1; // vI beats every 'h-line'
        }else{
//            System.out.println("opponent tries against 0 + m = " + mH[i]);
            if(1 == verticalLineIsFaster(mV[0], pV, mH[i], pH))
                return 0;
//            System.out.println("opponent tries against 1");
            if(1 == verticalLineIsFaster(mV[1], pV, mH[i], pH))
                return 1;
//            System.out.println("opponent tries against 2");
            if(1 == verticalLineIsFaster(mV[2], pV, mH[i], pH))
                return 2;
//            System.out.println("opponent tries against 3");
            if(1 == verticalLineIsFaster(mV[3], pV, mH[i], pH))
                return 3;
//            System.out.println("opponent beats player");
            return -1; // hI beats every 'v-line'
        }
    }
    
    /*figures out, which line would win (be faster at the intersection
      of both). both lines are defined with m and b this way:
      v(x) = mV * x + bV
      h(x) = mH * x + bH
      
      It is the turn of one line, so this line is 2^2 + 1^1 = 5 ahead
      (in squared distance; everything is measured in squared distance to
      avoid calculation which the square root which is performance intensive)
      
      If you do the math you get the following formulas for the intersection point
      (sX | sY):
      sX = (bH - bV) / (mV - mH)  
      sY = mV * (bH - bV)/(mV - mH) + bV
            for mV - mH != 0; otherwise the lines are parallel
      
      This method first has to check if one line intersects the (already existing)
      other graph (it is assumed that the vert graph goes staright upwards from
      fOfVertLine and the hor graph goes straight rightwards from fOfHorLine).
      If so, the other player's line is obviously faster (if both, return 0)
      Formular to check:
      if(h(xV) = mH * xV + bH >= yV) -> hor line intersects vert graph
      if((yH - bV) / mV       >= xH) -> vert line intersects hor graph
    
      If no graph is intersected, then the intersection point of v (vert line)
      and h (hor line) is calculated, the distances to the end points are compared 
      and the faster player is returned (1 -> vert is faster; 
      false -> hor is faster)
      If h and v are parallel, then 
    */
    private static int verticalLineIsFaster(float mV, Field fOfVertLine,
            float mH, Field fOfHorLine){
        float bV = fOfVertLine.getY() - mV * fOfVertLine.getX();
        float bH = fOfHorLine.getY() - mH * fOfHorLine.getX();
        
        //check for intersection of graph
        boolean vertLineIntersectsHorGraph = false;
        boolean horLineIntersectsVertGraph = false;
        if((fOfHorLine.getY() - bV) / mV >= fOfHorLine.getX()){
//            System.out.println("vert line intersects hor graph");
            vertLineIntersectsHorGraph = true;
        }
        if(mH * fOfVertLine.getX() + bH >= fOfVertLine.getY()){
//            System.out.println("hor line intersects vert graph");
            horLineIntersectsVertGraph = true;
        }
        if(horLineIntersectsVertGraph && vertLineIntersectsHorGraph)
            return 0;
        
        else if(horLineIntersectsVertGraph){
            return 1;
            
        }else if(vertLineIntersectsHorGraph){
            return -1;
        }
        
        //check for parallelism
        if(mV == mH){
//            System.out.println("lines are parallel");
           return 0; 
        }
        
        //calc intersection point
        float sX = (bH - bV) / (mV - mH);
        float sY = mV * (bH - bV)/(mV - mH) + bV;
        
        //check if S is in the 'relevant zone':
        //1. it has to be on the board (sX > 0 && sY > 0; equal to 0 means that
        //it cannot be a intersection point, because it is only playable for one)
        //2. it has to be 'below' fOfVertLine and fOfHorLine, not on the irrelevant
        //part of the board 'behind' the conflictzone, e. g. in (23|23).
        //So sY < fOfVertLine.getY() && sX < fOfHorLine.getX()
        if(sX <= 0 || sY <= 0 || sY >= fOfVertLine.getY() || sX >= fOfHorLine.getX()){
//            System.out.println("intersection point is not in relevant area);
            return 0;//equivalent to parallelism for further calculations
        }
        
//        System.out.println("Intersection point: (" + sX + "|" + sY + ")");
        
        //calc distances
//        float distFVertToS = getSquaredDistance(fOfVertLine, sX, sY);
//        float distFHorToS  = getSquaredDistance(fOfHorLine, sX, sY);
        
        
        float deltaXVert = Math.abs(sX - fOfVertLine.getX());
        float deltaYVert = Math.abs(sY - fOfVertLine.getY());
        int neededMovesVert = (int) Math.ceil(Math.min(deltaXVert, deltaYVert));
//        System.out.println("needed moves vert: " + neededMovesVert);
        float deltaXHor = Math.abs(sX - fOfHorLine.getX());
        float deltaYHor = Math.abs(sY - fOfHorLine.getY());
        int neededMovesHor = (int) Math.ceil(Math.min(deltaXHor, deltaYHor));
//        System.out.println("needed moves hor: " + neededMovesHor);
        
        //It is always verts turn (look at beginning of evaluateCurrentConflictzone)
        if(neededMovesVert <= neededMovesHor)
            return 1;
        else
            return -1;
    }
    
    private static float calcPointsWithLine(int index, Field f, boolean isVert){
        if(isVert){
            //calc intersection point with g(x) = 1 ('x axis plus 1 -> nearly 'Nullstelle')
            if(f.getY() > 0)
                return (1 + mV[index] * f.getX() - f.getY()) / mV[index];
            else
                return MAX_VALUE;//border reached -> line finished
        }else{
            //calc intersection point with y axis + 1 (x = 1) -> 'Y-Achsenabschnitt' + m
            if(f.getX() > 0)
                return f.getY() - mH[index] * f.getX() + mH[index] ;
            else
                return MAX_VALUE;//border reached -> line finished
        }
    }
    
    private static float getSquaredDistance(Field a, Field b){
        return (float) (Math.pow(Math.abs(b.getX() - a.getX()), 2) + 
                Math.pow(Math.abs(b.getY() - a.getY()), 2));
    }
    
    private static float getSquaredDistance(Field a, float bX, float bY){
        return (float) (Math.pow(Math.abs(bX - a.getX()), 2) + 
                Math.pow(Math.abs(bY - a.getY()), 2));
    }
    
    private static float getDistance(Field a, float bX, float bY){
        return (float) Math.sqrt(Math.pow(Math.abs(bX - a.getX()), 2) + 
                Math.pow(Math.abs(bY - a.getY()), 2));
    }
}
