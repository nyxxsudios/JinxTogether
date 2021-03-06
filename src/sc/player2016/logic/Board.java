package sc.player2016.logic;

import ch.qos.logback.classic.util.ContextInitializer;
import java.util.ArrayList;
import java.util.List;

import sc.player2016.logic.Jinx.FieldColor;
import sc.plugin2016.GameState;
import sc.plugin2016.Move;

public class Board {
    Field[][] fields = new Field[24][24];
    
    //vertical player starts the game
    //set in updateMove and undoMove, read in evaluateCurrentConfliczone
    boolean isJinxTurn = Jinx.jinxIsPlayingVertical;
    
    //is needed in undoMove, defined in constructor
    FieldColor vertLightColor;
    FieldColor horLightColor;
    
    //graphs by jinx sorted by points (first graph has most)
    ArrayList<Graph> graphsByJinx = new ArrayList<>();
    //graphs by opponent sorted by points (first graph has most)
    ArrayList<Graph> graphsByOpponent = new ArrayList<>();

    public Board(GameState gameStateAtBeginning){
            initFields(gameStateAtBeginning);
            blackField = new Field(-1,-1);
            blackField.setFieldColor(FieldColor.BLACK);
            if(Jinx.jinxIsPlayingVertical){
                vertLightColor = FieldColor.LIGHT_JINX;
                horLightColor = FieldColor.LIGHT_OPPONENT;
            }else{
                vertLightColor = FieldColor.LIGHT_OPPONENT;
                horLightColor = FieldColor.LIGHT_JINX;
            }
    }

    public Field getField(int x, int y){
            return fields[x] [y];
    }

    //just initialized once in constructor
    //used in getFieldSave
    private Field blackField;
    
    //used in Evaluator class
    public Field getFieldSave(int x, int y){
        if(x < 0 || x > 23 || y < 0 || y > 23){
            return blackField;
        }else{
            return fields[x][y];
        }
    }
    
    public void updateBoard(Field move, boolean isJinxMove){
            FieldColor fieldColor;
            
            if(isJinxMove){
                    fieldColor = FieldColor.JINX;
            }else{
                    fieldColor = FieldColor.OPPONENT; 
            }

            int x = move.getX();
            int y = move.getY();

            //setFieldColor
            move.setFieldColor(fieldColor);

            //check for new connections--------------------------
            final int[][] possibleFields = {
                            {-2, -1}, {-2, 1},
                            {-1, -2}, {-1, 2},
                            { 1, -2}, { 1, 2},
                            { 2, -1}, { 2, 1}
            };
            boolean isFirstConnectionAlreadyAdded = false;
            int indexOfMainGraph = 0;//0 just to make netbeans happy
            ArrayList<Graph> graphsByCurrentPlayer = isJinxMove?graphsByJinx:graphsByOpponent;
            for(int[] f : possibleFields){
                int pX = x+f[0];
                int pY = y+f[1];

                //if 'possible-Connection-Field' is a real field (not out of the board)
                if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){

                    //if 'possible-Connection-Field' has fieldColor
                    if(getField(pX, pY).getFieldColor() == fieldColor){

                        //Check for connection of opponent (FieldColor other)
                        if(connectionIsPossibleBetween(new int[]{x,y}, new int[]{pX, pY})){

                            //add connection to both fields
                            move.addConnectionTo(getField(pX, pY));
                            getField(pX, pY).addConnectionTo(move);

                            //if this connection is the first one
                            if(!isFirstConnectionAlreadyAdded){
                                for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                                    if(graphsByCurrentPlayer.get(i).containsField(getField(pX, pY))){
                                        graphsByCurrentPlayer.get(i).addField(move);
                                        indexOfMainGraph = moveGraphDownToRightPosition(i, isJinxMove);
                                        break;
                                    }
                                }
                                isFirstConnectionAlreadyAdded = true;
                                
                            }else{//second or higher connection in this updateBoard call
                                 for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                                     if(graphsByCurrentPlayer.get(i).containsField(getField(pX, pY))){
                                        if(i != indexOfMainGraph){//don't add again if last connection was to the same graph
                                            graphsByCurrentPlayer.get(indexOfMainGraph)
                                                    .addGraph(graphsByCurrentPlayer.get(i));
                                            graphsByCurrentPlayer.remove(i);
                                            if(indexOfMainGraph > i){ indexOfMainGraph--; }
                                            indexOfMainGraph = moveGraphDownToRightPosition(indexOfMainGraph, isJinxMove);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }//end check for new connections

            if(!isFirstConnectionAlreadyAdded){//no connections added at all
                graphsByCurrentPlayer.add(new Graph(move));
            }
            
            isJinxTurn = !isJinxTurn;
    }

    public void undoMove(Field move, boolean isJinxMove){
        
            ArrayList<Graph> graphsByCurrentPlayer = isJinxMove?graphsByJinx:graphsByOpponent;
            
            //reset field color
            if(move.getX() == 0 || move.getX() == 23){
                move.setFieldColor(horLightColor);
                
            }else if(move.getY() == 0 || move.getY() == 23){
                move.setFieldColor(vertLightColor);
                
            }else{
                move.setFieldColor(FieldColor.BLACK);
            }

            //recalculate graphs
            //if the field had at least 1 connection
            if(move.getConnections().size() > 0){
                for(int i=0; i<graphsByCurrentPlayer.size(); i++){
                     if(graphsByCurrentPlayer.get(i).containsField(move)){
                         
                         ArrayList<Graph> newGraphs = graphsByCurrentPlayer.get(i).removeField(move);
                         
                         moveGraphUpToRightPosition(i, isJinxMove);
                         for(Graph g : newGraphs){
                             graphsByCurrentPlayer.add(g);
                             moveGraphDownToRightPosition(graphsByCurrentPlayer.size()-1, isJinxMove);
                         }
                         break;
                     }    
                }
            }else{
                for(int i=graphsByCurrentPlayer.size()-1; i>= 0; i--){
                    if(graphsByCurrentPlayer.get(i).containsField(move)){
                        graphsByCurrentPlayer.remove(i);
                        break;
                    }
                }
            }
            
            isJinxTurn = !isJinxTurn;
    }

    public float evaluateBoardPosition(){
        if(Jinx.jinxIsPlayingVertical){
            return Evaluator.evaluateBoardPosition(graphsByJinx, graphsByOpponent, 
                    isJinxTurn, graphsByJinx.get(0).getPoints(Jinx.jinxIsPlayingVertical),
                    graphsByOpponent.get(0).getPoints(!Jinx.jinxIsPlayingVertical));
        }else{
            return Evaluator.evaluateBoardPosition(graphsByOpponent, graphsByJinx, 
                    !isJinxTurn, graphsByJinx.get(0).getPoints(Jinx.jinxIsPlayingVertical),
                    graphsByOpponent.get(0).getPoints(!Jinx.jinxIsPlayingVertical));
        }
    }
    
    public float evaluateConflict(){
        if(Jinx.jinxIsPlayingVertical){
            if(isJinxTurn){
                System.out.println("is jinx turn");
                return Evaluator.evaluateVertsConflictzones(graphsByJinx, graphsByOpponent, true);
            }
            System.out.println("is opponents turn");
            return Evaluator.evaluateHorsConflictzones(graphsByJinx, graphsByOpponent, true);
        }else{
            if(isJinxTurn){
                System.out.println("is jinx turn");
                return Evaluator.evaluateHorsConflictzones(graphsByOpponent, graphsByJinx, true);
            }
            System.out.println("is opponents turn");
            return Evaluator.evaluateVertsConflictzones(graphsByOpponent, graphsByJinx, true);
        }
    }
    
    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a senseful depth)
    public ArrayList<Field> preselectMovesOld(Field lastMove, Field secondLastMove){

            int x = lastMove.getX();
            int y = lastMove.getY();

            ArrayList<Field> result = new ArrayList<Field>();

            final int[][] goodFields = { { 0,-4},
                                {-1,-3}, { 0,-3}, { 1,-3}, 
                       {-2,-2}, {-1,-2}, { 0,-2}, { 1,-2}, { 2,-2},
              {-3,-1}, {-2,-1}, {-1,-1}, { 0,-1}, { 1,-1}, { 2,-1}, { 3,-1},
     {-4, 0}, {-3, 0}, {-2, 0}, {-1, 0},          { 1, 0}, { 2, 0}, { 3, 0}, { 4, 0},
              {-3, 1}, {-2, 1}, {-1, 1}, { 0, 1}, { 1, 1}, { 2, 1}, { 3, 1},
                       {-2, 2}, {-1, 2}, { 0, 2}, { 1, 2}, { 2, 2},
                                {-1, 3}, { 0, 3}, { 1, 3},
                                         { 0, 4}
        };

//            final int[][] goodFields2 = {                                    {0, -4},
//                                                                    {-1, -3}, {0, -3}, {1, -3}, 
//                                              {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
//                            {-3, -1}, {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1}, {3, -1},
//                            {-4,0}, {-3,  0}, {-2,  0}, {-1,  0},          {1,  0}, {2,  0}, {3,  0}, {4,0},
//                            {-3,  1}, {-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1}, {3,  1},
//                                              {-2,  2}, {-1,  2}, {0,  2}, {1,  2}, {2,  2},
//                                                                    {-1,  3}, {0,  3}, {1,  3},
//                                                                    {0, 4}
//            };

//                final int[][] goodFields = {
//                                                              {0, -3}, 
//                                                    {-1, -2}, {0, -2}, {1, -2}, 
//                                          {-2, -1}, {-1, -1}, {0, -1}, {1, -1}, {2, -1},
//				{-3,  0}, {-2,  0}, {-1, 0},          {1,  0}, {2,  0}, {3,  0},
//                                          {-2,  1}, {-1,  1}, {0,  1}, {1,  1}, {2,  1},
//                                                    {-1,  2}, {0,  2}, {1,  2}, 
//                                                              {0,  3}
//		};

//		final int[][] goodFields = {
//				{-3, -1}, {-3,  0}, {-3,  1},
//				{3,  -1}, { 3,  0}, { 3,  2},
//				{-2, -2}, {-2, -1}, {-2,  0}, {-2,  1}, {-2,  2}, 
//				{ 2, -2}, { 2, -1}, { 2,  0}, { 2,  1}, { 2,  2},
//				{-1, -3}, {-1, -2}, {-1, -1}, {-1,  0}, {-1,  1}, {-1,  2}, {-1,  3},
//				{ 1, -3}, { 1, -2}, { 1, -1}, { 1,  0}, { 1,  1}, { 1,  2}, { 1,  3},
//				{ 0, -3}, { 0, -2}, { 0, -1}, 		    { 0,  1}, { 0,  2}, { 0,  3},
//		};

            int pX, pY;
            //second last move was a move by the current player
            final int[][] goodFieldsFromSecondLastMove = {                    
                                            {0, -4},	
                    {-3, -3},                                       {3, -3},
                                    {-1, -2},       {1, -2}, 
                            {-2, -1},                       {2, -1},
            {-4, 0},                                                        {4, 0},
                            {-2,  1},                       {2,  1},
                                    {-1,  2},       {1,  2}, 
                    {-3,  3},                                       {3,  3},
                                            {0,  4}
            };

            //get fields around lastMove
            for(int[] f : goodFields){
                    pX = x+f[0];
                    pY = y+f[1];
                    if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){
                            if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                                    result.add(getField(pX,pY));
                            }
                    }else{
//				System.out.println("Out of Bounds!\n"
//						+ "lastField (" + x + ", " + y + "); f = (" + pX + ", " + pY + ")") ;	
                    }
            }

            //get fields around secondLastMove
            if(secondLastMove != null){//if it is at least turn 3
                    x = secondLastMove.getX();
                    y = secondLastMove.getY();

                    for(int[] f : goodFieldsFromSecondLastMove){
                            pX = x+f[0];
                            pY = y+f[1];
                            if(pX >= 0 && pX < 24 && pY >= 0 && pY < 24){
                                    if(getField(pX,pY).getFieldColor() == FieldColor.BLACK){
                                            if(!result.contains(getField(pX,pY))){
                                                    result.add(getField(pX,pY));
                                            }
                                    }
                            }else{
    //				System.out.println("Out of Bounds!\n"
    //						+ "lastField (" + x + ", " + y + "); f = (" + pX + ", " + pY + ")") ;	
                            }
                    }
            }
            assert(result.size() > 0);
            return result;
    }

    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a senseful depth)
    public ArrayList<Field> preselectMoves(Field lastMove, boolean isVertical){
        return Preselector.preselectMoves(lastMove, isVertical, this);
    }
    
    //checks if any other connection (by opponent or even by jinx) 
    //prevents a new connection between f1 and f2
    private boolean connectionIsPossibleBetween(int[] f1, int[] f2){
            //ASSERT f1 and f2 have 'knight-distance' (chess)

            int x1, y1, x2, y2;

            if(f1[0] > f2[0]){
                    //change f1 and f2
                    x1 = f2[0];
                    y1 = f2[1];
                    x2 = f1[0];
                    y2 = f1[1];
            }else{
                    x1 = f1[0];
                    y1 = f1[1];
                    x2 = f2[0];
                    y2 = f2[1];
            }

            final int[][][] connectionsToCheckInCase1 = {
                            {{1, 1}, {0, -1}},
                            {{1, 1}, {2, -1}},
                            {{1, 1}, {3,  0}},

                            {{0, 1}, {1, -1}},
                            {{0, 1}, {2,  0}},

                            {{2, 2}, {1,  0}},
                            {{1, 2}, {2,  0}},
                            {{0, 2}, {1,  0}},
                            {{-1, 1},{1,  0}},
            };
            final int[][][] connectionsToCheckInCase2 = {
                            {{1, 0}, {-1, -1}},
                            {{1, 0}, {0, -2}},
                            {{1, 0}, {2,  -2}},

                            {{2, 0}, {0, -1}},
                            {{2, 0}, {1,  -2}},

                            {{0, 1}, {1, -1}},
                            {{1, 1}, {0, -1}},
                            {{2, 1}, {1, -1}},
                            {{3, 0}, {1, -1}},
            };
            final int[][][] connectionsToCheckInCase3 = {
                            {{0, 1}, {1, -1}},
                            {{0, 1}, {2,  0}},
                            {{0, 1}, {2,  2}},

                            {{0, 2}, {1, 0}},
                            {{0, 2}, {2, 1}},

                            {{-1, 0}, {1, 1}},
                            {{-1, 1}, {1, 0}},
                            {{-1, 2}, {1, 1}},
                            {{ 0, 3}, {1, 1}},
            };
            final int[][][] connectionsToCheckInCase4 = {
                            {{1, -1}, {-1,  0}},
                            {{1, -1}, {-1, -2}},
                            {{1, -1}, { 0, -3}},

                            {{1, 0}, {-1, -1}},
                            {{1, 0}, { 0, -2}},

                            {{2, -2}, {0, -1}},
                            {{2, -1}, {0, -2}},
                            {{2,  0}, {0, -1}},
                            {{1,  1}, {0, -1}},
            };


            /*distinguish between 4 cases of the relation of f1 (x1, y1) and f2 (x2, y2). 
             * x1 < x2 is true per definition:
             */
            final int[][][] connectionsToCheck;
            if(x1+2 == x2 && y1+1 == y2){
                    connectionsToCheck = connectionsToCheckInCase1;

            }else if(x1+2 == x2 && y1-1 == y2){
                    connectionsToCheck = connectionsToCheckInCase2;

            }else if(x1+1 == x2 && y1+2 == y2){
                    connectionsToCheck = connectionsToCheckInCase3;

            }else if(x1+1 == x2 && y1-2 == y2){
                    connectionsToCheck = connectionsToCheckInCase4;

            }else{
                    connectionsToCheck = null;//make eclipse happy
                    System.out.println("ERROR, wrong arguments f1 and f2 passed! They are not in 'knight-distance'");
                    assert(false);
            }

            //check connections
            for(int[][] c : connectionsToCheck){
                    int xP1 = x1+c[0][0];
                    int yP1 = y1+c[0][1];
                    int xP2 = x1+c[1][0];
                    int yP2 = y1+c[1][1];

                    //if P1 and P2 are on the field
                    if(xP1 >= 0 && xP1 < 24 && yP1 >= 0 && yP1 < 24 && 
                       xP2 >= 0 && xP2 < 24 && yP2 >= 0 && yP2 < 24){

                        if(getField(xP1, yP1).isConnectedWith(xP2, yP2)){
                                return false;
                        }
                    }

            }
            return true;
    }
    
    private int moveGraphDownToRightPosition(int index, boolean isJinx){
        
        //move graph downwards (smaller indices, element 0 is worth most points)
        //in graphsByCurrentPlayer until it is at the right position
        if(isJinx){
            Graph g = graphsByJinx.get(index);
            graphsByJinx.remove(index);
            int points = g.getPoints(Jinx.jinxIsPlayingVertical);
            for(int i=index-1; i>=0; i--){
                if(graphsByJinx.get(i).getPoints(Jinx.jinxIsPlayingVertical) >= points){
                    graphsByJinx.add(i+1, g);
                    return i+1;
                }
            }
            graphsByJinx.add(0, g);
            return 0;
            
        }else{//is opponent
            Graph g = graphsByOpponent.get(index);
            graphsByOpponent.remove(index);
            int points = g.getPoints(!Jinx.jinxIsPlayingVertical);
            for(int i=index-1; i>=0; i--){
                if(graphsByOpponent.get(i).getPoints(!Jinx.jinxIsPlayingVertical) >= points){
                    graphsByOpponent.add(i+1, g);
                    return i+1;
                }
            }
            graphsByOpponent.add(0, g);
            return 0;
        }
//        return index;
    }
    
    private void moveGraphUpToRightPosition(int index, boolean isJinx){
        
        //move graph upwards (bigger indices, element 0 is worth most points)
        //in graphsByCurrentPlayer until it is at the right position
        if(isJinx){
            Graph g = graphsByJinx.get(index);
            graphsByJinx.remove(index);
            int points = g.getPoints(Jinx.jinxIsPlayingVertical);
            for(int i=index; i<graphsByJinx.size(); i++){
                if(graphsByJinx.get(i).getPoints(Jinx.jinxIsPlayingVertical) <= points){
                    graphsByJinx.add(i, g);
                    return;
                }
            }
            graphsByJinx.add(graphsByJinx.size(), g);
            
        }else{//is opponent
            Graph g = graphsByOpponent.get(index);
            graphsByOpponent.remove(index);
            int points = g.getPoints(!Jinx.jinxIsPlayingVertical);
            for(int i=index; i<graphsByOpponent.size(); i++){
                if(graphsByOpponent.get(i).getPoints(!Jinx.jinxIsPlayingVertical) <= points){
                    graphsByOpponent.add(i, g);
                    return;
                }
            }
            graphsByOpponent.add(graphsByOpponent.size(), g);
        }
    }
    
    private void initFields(GameState gameState){
            List<Move> possibleMoves = gameState.getPossibleMoves();

            //every field is green per default
            for(int row=0; row<24; row++){
                    for(int col=0; col<24; col++){
                            fields[row][col] = new Field(row, col);
                            fields[row][col].setFieldColor(FieldColor.GREEN);
                    }
            }

            //every possible field becomes black
            for(Move move : possibleMoves){
                    fields[move.getX()][move.getY()].setFieldColor(FieldColor.BLACK);
            }


            //Set LIGHT_JINX and LIGHT_OPPONENT (border) fields
            if(Jinx.jinxIsPlayingVertical){
                for(int i=1; i<= 22; i++){
                    fields[i][0].setFieldColor(FieldColor.LIGHT_JINX);
                    fields[i][23].setFieldColor(FieldColor.LIGHT_JINX);
                    fields[0][i].setFieldColor(FieldColor.LIGHT_OPPONENT);
                    fields[23][i].setFieldColor(FieldColor.LIGHT_OPPONENT);
                }
            }else{
                for(int i=1; i<= 22; i++){
                    fields[i][0].setFieldColor(FieldColor.LIGHT_OPPONENT);
                    fields[i][23].setFieldColor(FieldColor.LIGHT_OPPONENT);
                    fields[0][i].setFieldColor(FieldColor.LIGHT_JINX);
                    fields[23][i].setFieldColor(FieldColor.LIGHT_JINX);
                }
            }
    }
        
    int getNumberOfSetFields() {
        int result = 0;
        for(int row=0; row<24; row++){
            for(int col=0; col<24; col++){
                if(fields[col][row].getFieldColor() == FieldColor.JINX ||
                   fields[col][row].getFieldColor() == FieldColor.OPPONENT){
                    result++;
                }
            }
        }
        return result;
    }
}
