package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator

class MinMaxEngine(private val depth: Int, private val lines: Int) : Engine {
    class Node(val move: Move, var score: Float) {
        var children = mutableListOf<Node>()

        fun getScore(white: Boolean) : Float {
            if (children.isEmpty()) {
                return score
            }

            val scores = children.map { it.getScore(!white) }

            return if (white) {
                // min
                scores.minOrNull()!!
            } else {
                // max
                scores.maxOrNull()!!
            }
        }
    }

    init {
        println("MinMax engine\nDepth $depth, Lines $lines")
        println("Expect loads of lag.")
    }

    override fun genMove(board: Board): Move {
        // TODO: Intelligent move culling
        // TODO: Draw detection
        val tree = Node(Move(-1, -1), 0f)
        bestMove(board, depth, tree)
        val moves = tree.children.sortedBy { it.score }
        return if (board.whiteToMove) {
            moves.last().move
        } else {
            moves.first().move
        }
    }

    private fun bestMove(board: Board, currentDepth: Int, node: Node) {
        val moves = board.getPieces(board.whiteToMove).map { MoveGenerator.findMovesSmart(board, it) }.flatten().shuffled()

        moves.sortedBy {
            BoardEvaluator.score(board.move(it))
        }.subList((moves.size - lines).coerceAtLeast(0), moves.size).forEach {
            val new = Node(it, 0f)
            val newBoard = board.move(it)

            if (currentDepth == 1) {
                // Raw numbers
                new.score = BoardEvaluator.score(newBoard)
            } else {
                // handle children
                bestMove(newBoard, currentDepth - 1, new)
                new.score = new.getScore(board.whiteToMove)
            }

            node.children.add(new)
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
