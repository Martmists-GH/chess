package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator

class MinMaxEngine(private val depth: Int, private val lines: Int) : Engine {
    init {
        println("MinMax engine\nDepth $depth, Lines $lines")
    }

    override fun genMove(board: Board): Move {
        return bestMove(board, depth)
    }

    private fun bestMove(board: Board, currentDepth: Int) : Move {
        val moves = board.getPieces(board.whiteToMove).map { MoveGenerator.findMovesSmart(board, it) }.flatten()

        if (moves.size == 1) {
            return moves[0]
        }

        if (depth - currentDepth == 1) {
            return bestMoveScore(board, moves, board.whiteToMove)
        } else {
            val sorted = moves.sortedBy {
                BoardEvaluator.score(board.move(it))
            }.subList((moves.size - lines).coerceAtLeast(0), moves.size).sortedBy {
                val newBoard = board.move(it)
                newBoard.checkState()

                if (newBoard.gameEnded) {
                    return@sortedBy if (newBoard.whiteToMove) {
                        -1000f
                    } else {
                        1000f
                    }
                }

                val opponentMove = bestMove(newBoard, currentDepth-1)
                BoardEvaluator.score(newBoard.move(opponentMove))
            }

            return if (board.whiteToMove) {
                sorted.last()
            } else {
                sorted.first()
            }
        }
    }

    private fun bestMoveScore(board: Board, moves: List<Move>, white: Boolean) : Move {
        val ordered = moves.sortedBy { BoardEvaluator.score(board.move(it)) }
        return if (white) {
            ordered.last()
        } else {
            ordered.first()
        }
    }
}
