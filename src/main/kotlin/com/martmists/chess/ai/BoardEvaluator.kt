package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator
import com.martmists.chess.game.PieceType

object BoardEvaluator {
    private val cache = HashMap<Int, Float>()

    fun score(board: Board) : Float {
        return cache.getOrPut(board.hashCode()) {
            val whitePieces = board.getPieces(true)
            val blackPieces = board.getPieces(false)
            pieces(board, whitePieces, blackPieces) -
                pawns(board, whitePieces, blackPieces) +
                mobility(board, whitePieces, blackPieces)
        }
    }

    private fun mobility(board: Board, whitePieces: List<Int>, blackPieces: List<Int>) : Float {
        val whiteMoves = whitePieces.map { MoveGenerator.findMovesSmart(board, it) }.flatten().size
        val blackMoves = blackPieces.map { MoveGenerator.findMovesSmart(board, it) }.flatten().size
        return 0.1f * (whiteMoves - blackMoves)
    }

    private fun pawns(board: Board, whitePieces: List<Int>, blackPieces: List<Int>) : Float {
        val whitePawns = whitePieces.filter { board.pieces[it].type == PieceType.PAWN }
        val blackPawns = blackPieces.filter { board.pieces[it].type == PieceType.PAWN }

        return 0.5f * (
                doubled(board, whitePawns, blackPawns) +
                blocked(board, whitePawns, blackPawns) +
                isolated(board, whitePawns, blackPawns))
    }

    private fun doubled(board: Board, whitePawns: List<Int>, blackPawns: List<Int>) : Int {
        val white = whitePawns.map { Move.file(it) }
        val black = blackPawns.map { Move.file(it) }

        var whiteDoubled = 0
        var blackDoubled = 0
        val files = "abcdefgh"
        for (x in 0 until 8) {
            val wc = white.filter { it == files[x] }
            val bc = black.filter { it == files[x] }
            if (wc.size > 1) {
                whiteDoubled += wc.size
            }
            if (bc.size > 1) {
                blackDoubled += bc.size
            }
        }

        return whiteDoubled - blackDoubled
    }

    private fun blocked(board: Board, whitePawns: List<Int>, blackPawns: List<Int>) : Int {
        val whiteBlocked = whitePawns.filter { MoveGenerator.findMovesSmart(board, it).isEmpty() }.size
        val blackBlocked = blackPawns.filter { MoveGenerator.findMovesSmart(board, it).isEmpty() }.size
        return whiteBlocked - blackBlocked
    }

    private fun isolated(board: Board, whitePawns: List<Int>, blackPawns: List<Int>) : Int {
        var wi = 0
        var bi = 0

        for (x in 1..8) {
            val wp = whitePawns.filter { it % 10 == x }.size
            val wa = whitePawns.filter { it % 10 == x-1 || it % 10 == x+1 }.size
            val bp = blackPawns.filter { it == x }.size
            val ba = blackPawns.filter { it == x }.size

            if (wa == 0 && wp != 0) {
                wi += wp
            }
            if (ba == 0 && bp != 0) {
                bi += bp
            }
        }
        return wi - bi
    }

    /**
     * Just count piece advantage
     */
    private fun pieces(board: Board, whitePieces: List<Int>, blackPieces: List<Int>) : Float {
        val white = whitePieces.sumOf { value(board, it).toBigDecimal() }
        val black = blackPieces.sumOf { value(board, it).toBigDecimal() }
        return (white - black).toFloat()
    }

    private fun value(board: Board, index: Int, ignoreKings: Boolean = false) : Float {
        return when (board.pieces[index].type) {
            PieceType.PAWN -> 1f
            PieceType.BISHOP -> 3.3f
            PieceType.KNIGHT -> 3.2f
            PieceType.ROOK -> 5f
            PieceType.QUEEN -> 9f
            PieceType.KING -> if (ignoreKings) 0f else 200f
            else -> 0f
        }
    }
}