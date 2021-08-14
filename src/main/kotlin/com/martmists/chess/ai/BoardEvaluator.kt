package com.martmists.chess.ai

import com.martmists.chess.game.Board
import com.martmists.chess.game.Move
import com.martmists.chess.game.MoveGenerator
import com.martmists.chess.game.PieceType

object BoardEvaluator {
    fun score(board: Board) : Float {
        val whitePieces = board.getPieces(true)
        val blackPieces = board.getPieces(false)
        return pieces(board, whitePieces, blackPieces) - pawns(board, whitePieces, blackPieces) + mobility(board, whitePieces, blackPieces)
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
        // TODO
        return 0
    }

    /**
     * Just count piece advantage
     */
    private fun pieces(board: Board, whitePieces: List<Int>, blackPieces: List<Int>) : Int {
        val white = whitePieces.sumOf { value(board, it) }
        val black = blackPieces.sumOf { value(board, it) }
        return white - black
    }

    private fun value(board: Board, index: Int, ignoreKings: Boolean = false) : Int {
        return when (board.pieces[index].type) {
            PieceType.PAWN -> 1
            PieceType.BISHOP, PieceType.KNIGHT -> 3
            PieceType.ROOK -> 5
            PieceType.QUEEN -> 9
            PieceType.KING -> if (ignoreKings) 0 else 200
            else -> 0
        }
    }
}