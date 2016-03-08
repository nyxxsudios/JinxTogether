/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sc.player2016.logic;

import java.util.ArrayList;

/**
 *
 * @author Jonas
 */
public class Preselector {
    
    //important part of the Jinx AI. Returns all 'good' moves
    //that can be done (returning all possible moves would be too much
    //to calculate in a useful depth)
    public static ArrayList<Field> preselectMoves(Field lastMove, boolean isVertical, Board board){
        ArrayList<Field> result = new ArrayList<>();
        Field notCurrentPlayerMin, notCurrentPlayerMax, help;//notCurrentPlayer (can be jinx!)
        ArrayList<Graph> graphsByCurrentPlayer;
        int x, y, pX, pY;
         
        if(isVertical){//preselect for vertical player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = board.graphsByJinx;
                notCurrentPlayerMax = board.graphsByOpponent.get(0).getMaxXField();
                notCurrentPlayerMin = board.graphsByOpponent.get(0).getMinXField();
            }else{
                graphsByCurrentPlayer = board.graphsByOpponent;
                notCurrentPlayerMax = board.graphsByJinx.get(0).getMaxXField();
                notCurrentPlayerMin = board.graphsByJinx.get(0).getMinXField();
            }

            final int[][] goodFieldsFromOwnMinY = {                    
                                                      {0, -4},	
                         {-3, -3},                                              {3, -3},
    //                                      {-1, -2},         {1, -2},      //already added before
                                   {-2, -1},                           {2, -1},
                {-4, 0},                                                                {4, 0},
            };

            final int[][] goodFieldsFromOwnMaxY = {                    
                {-4, 0},                                                                {4, 0},
                                  {-2,  1},                            {2,  1},
    //                                      {-1,  2},         {1,  2},  //already added before
                         {-3,  3},                                              {3,  3}, 
                                                      {0,  4}
            };

            final int[][] goodFieldsReactToOpponentMove = {
    //                                                {-1,-3}, { 0,-3}, { 1,-3}, 
                                    {-3,-3},                                            { 3,-3},
                                            {-2,-1}, {-1,-1}, { 0,-1}, { 1,-1}, { 2,-1},
                            {-4, 0},                 {-1, 0},          { 1, 0},                 { 4, 0},
                                            {-2, 1}, {-1, 1}, { 0, 1}, { 1, 1}, { 2, 1},
                                    {-3, 3},                                            { 3, 3},
    //                                                {-1, 3}, { 0, 3}, { 1, 3},
    //                                                         { 0, 4}
            };

            //add (maximum) 4 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add two fields to result for minYField
                if(g.getMinYField().getY() - 2 >= 0){
                    if(g.getMinYField().getX() - 1 > 0){
                        help = board.getField(g.getMinYField().getX()-1, g.getMinYField().getY()-2);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMinYField().getX() + 1 < 23){
                        help =board.getField(g.getMinYField().getX()+1, g.getMinYField().getY()-2);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                //add two fields to result for maxYField
                if(g.getMaxYField().getY() + 2 <= 23){
                    if(g.getMaxYField().getX() - 1 > 0){
                        help = board.getField(g.getMaxYField().getX()-1, g.getMaxYField().getY()+2);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMaxYField().getX() + 1 < 23){
                        help = board.getField(g.getMaxYField().getX()+1, g.getMaxYField().getY()+2);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minY
            x = graphsByCurrentPlayer.get(0).getMinYField().getX();
            y = graphsByCurrentPlayer.get(0).getMinYField().getY();
            for(int[] f : goodFieldsFromOwnMinY){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxY
            x = graphsByCurrentPlayer.get(0).getMaxYField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxYField().getY();
            for(int[] f : goodFieldsFromOwnMaxY){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }

            //add fields from lastMove (opponent)
            x = lastMove.getX();
            y = lastMove.getY();
            for(int[] f : goodFieldsReactToOpponentMove){
                pX = x+f[0];
                pY = y+f[1];
                if(pX > 0 && pX < 23 && pY >= 0 && pY < 24){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }   
            
            //add fields from start and end of notCurrentPlayer
            if(notCurrentPlayerMin.getX() - 4 > 0){
                help = board.getField(notCurrentPlayerMin.getX() - 4, notCurrentPlayerMin.getY());
                if(help.getFieldColor() == Jinx.FieldColor.BLACK){
                    if(!result.contains(help)){
                        result.add(help);
                    }
                }
            }
            if(notCurrentPlayerMax.getX() + 4 < 23){
                help = board.getField(notCurrentPlayerMax.getX() + 4, notCurrentPlayerMax.getY());
                if(help.getFieldColor() == Jinx.FieldColor.BLACK){
                    if(!result.contains(help)){
                        result.add(help);
                    }
                }
            }

            return result;
            
        }else{//preselect for horizontal player
            
            if(Jinx.jinxIsPlayingVertical){
                graphsByCurrentPlayer = board.graphsByOpponent;
                notCurrentPlayerMax = board.graphsByJinx.get(0).getMaxYField();
                notCurrentPlayerMin = board.graphsByJinx.get(0).getMinYField();
            }else{
                graphsByCurrentPlayer = board.graphsByJinx;
                notCurrentPlayerMax = board.graphsByOpponent.get(0).getMaxYField();
                notCurrentPlayerMin = board.graphsByOpponent.get(0).getMinYField();
            }

            final int[][] goodFieldsFromOwnMinX = {                    
                                                  {0, -4},	
                        {-3, -3},                                   
                                        {-1, -2},      

                {-4, 0},                                             

                                        {-1,  2},      
                        {-3,  3},                                      
                                                  {0,  4}
                };

            final int[][] goodFieldsFromOwnMaxX = {                    
                {0, -4},	
                                    {3, -3},
                      {1, -2}, 

                                            {4, 0},

                      {1,  2}, 
                                    {3,  3},
                {0,  4}
            };

            final int[][] goodFieldsReactToOpponentMove = {
                                            { 0,-4},
                        {-3, -3},                                   { 3,-3},
                                   {-1,-2},          { 1,-2},
                                   {-1,-1}, { 0,-1}, { 1,-1}, 
                                   {-1, 0},          { 1, 0},
                                   {-1, 1}, { 0, 1}, { 1, 1},
                                   {-1, 2},          { 1, 2},
                        {-3, 3},                                    { 3, 3},
                                            { 0, 4}
            };

            //add (maximum) 4 fields for each graph (possible connections to other graphs)
            for(Graph g : graphsByCurrentPlayer){
//                if(g.hasJustOneField())break;
                //add two fields to result for minXField
                if(g.getMinXField().getX() - 2 >= 0){
                    if(g.getMinXField().getY() - 1 > 0){
                        help = board.getField(g.getMinXField().getX()-2, g.getMinXField().getY()-1);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMinXField().getY() + 1 < 23){
                        help = board.getField(g.getMinXField().getX()-2, g.getMinXField().getY()+1);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
                //add two fields to result for maxXField
                if(g.getMaxXField().getX() + 2 <= 23){
                    if(g.getMaxXField().getY() - 1 > 0){
                        help = board.getField(g.getMaxXField().getX()+2, g.getMaxXField().getY()-1);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                    if(g.getMaxXField().getY() + 1 < 23){
                        help = board.getField(g.getMaxXField().getX()+2, g.getMaxXField().getY()+1);
                        if(help.getFieldColor() == Jinx.FieldColor.BLACK
                                && !result.contains(help)){
                            result.add(help);
                        }
                    }
                }
            }

            //add fields for start and end field of own graph
            //get fields from minX
            x = graphsByCurrentPlayer.get(0).getMinXField().getX();
            y = graphsByCurrentPlayer.get(0).getMinXField().getY();
            for(int[] f : goodFieldsFromOwnMinX){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }
            //get fields from maxY
            x = graphsByCurrentPlayer.get(0).getMaxYField().getX();
            y = graphsByCurrentPlayer.get(0).getMaxYField().getY();
            for(int[] f : goodFieldsFromOwnMaxX){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }

            //add fields from lastMove (opponent)
            x = lastMove.getX();
            y = lastMove.getY();
            for(int[] f : goodFieldsReactToOpponentMove){
                pX = x+f[0];
                pY = y+f[1];
                if(pX >= 0 && pX < 24 && pY > 0 && pY < 23){
                    if(board.getField(pX,pY).getFieldColor() == Jinx.FieldColor.BLACK){
                        if(!result.contains(board.getField(pX,pY))){
                                result.add(board.getField(pX,pY));
                        }
                    }
                }
            }   
            
            //add fields from start and end of notCurrentPlayer (vertical)
            if(notCurrentPlayerMin.getY() - 4 > 0){
                help = board.getField(notCurrentPlayerMin.getX(), notCurrentPlayerMin.getY() - 4);
                if(help.getFieldColor() == Jinx.FieldColor.BLACK){
                    if(!result.contains(help)){
                        result.add(help);
                    }
                }
            }
            if(notCurrentPlayerMax.getY() + 4 < 23){
                help = board.getField(notCurrentPlayerMax.getX(), notCurrentPlayerMax.getY() + 4);
                if(help.getFieldColor() == Jinx.FieldColor.BLACK){
                    if(!result.contains(help)){
                        result.add(help);
                    }
                }
            }

            return result;
        }
    }
    
}
