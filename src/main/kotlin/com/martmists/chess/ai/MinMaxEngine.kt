package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator
import com.martmists.chess.utilities.LruCache
import kotlin.math.max
import kotlin.math.min

class MinMaxEngine(private val depth: Int) : Engine {
    private val nullCutoff = 2.3f

    init {
        println("MinMax engine\nDepth $depth")
        println("Expect loads of lag.")
    }

    override fun genMove(board: Board): Move {
        val moves = board.getPieces(board.whiteToMove).map { MoveGenerator.findMovesSmart(board, it) }.flatten()

        // generate now, not later
        val values = moves.associateWith {
            alphabeta(board.move(it), depth-1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        val sorted = moves.sortedBy { values[it]!! }

        return if (board.whiteToMove) {
            sorted.last()
        } else {
            sorted.first()
        }
    }

    private val cache = LruCache<Int, Float>(500)

    // Alpha-Beta pruning
    private fun alphabeta(board: Board, currentDepth: Int, _alpha: Float, _beta: Float) : Float {
        return cache.getOrPut(board.hashCode()) {
            val currentScore = BoardEvaluator.score(board)

            if (currentDepth == 0) {
                currentScore
            } else {

                var alpha = _alpha
                var beta = _beta

                val moves = board.getPieces(board.whiteToMove).map { MoveGenerator.findMovesSmart(board, it) }.flatten().shuffled()

                if (board.whiteToMove) {
                    var value = Float.NEGATIVE_INFINITY
                    for (move in moves) {
                        val nextBoard = board.move(move)

                        if (currentScore - BoardEvaluator.score(nextBoard) > nullCutoff) {
                            continue
                        }

                        value = max(value, alphabeta(nextBoard, currentDepth - 1, alpha, beta))
                        if (value >= beta) {
                            break
                        }
                        alpha = max(alpha, value)
                    }
                    value
                } else {
                    var value = Float.POSITIVE_INFINITY
                    for (move in moves) {
                        val nextBoard = board.move(move)

                        if (currentScore - BoardEvaluator.score(nextBoard) < -nullCutoff) {
                            continue
                        }

                        value = min(value, alphabeta(board.move(move), currentDepth - 1, alpha, beta))
                        if (value <= alpha) {
                            break
                        }
                        beta = min(beta, value)
                    }
                    value
                }
            }
        }
    }

    override fun reset() {

    }
}
