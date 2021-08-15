package com.martmists.chess.game

import com.martmists.chess.utilities.LruCache
import java.util.*
import kotlin.math.abs

object MoveGenerator {
    private val cache = LruCache<Int, MutableMap<Int, List<Move>>>(300)

    private val KING_MOVES = listOf(-11, -10, -9, -1, 1, 9, 10, 11)
    private val BISHOP_MOVES = listOf(-11, -9, 9, 11)
    private val KNIGHT_MOVES = listOf(-21, -19, -12, -8, 8, 12, 19, 21)
    private val ROOK_MOVES = listOf(-10, -1, 1, 10)
    private val QUEEN_MOVES = listOf(-11, -10, -9, -1, 1, 9, 10, 11)
    private val PROMOTION = listOf(PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN)

    /**
     * Does not detect check or pins
     */
    fun findMovesSimple(board: Board, index: Int) : List<Move> {
        val piece = board.pieces[index]
        val possibleMoves = mutableListOf<Move>()

        when (piece.type) {
            PieceType.INVALID, PieceType.EMPTY -> return listOf()
            PieceType.PAWN -> {
                val moveDirection = if (piece.white) 10 else -10
                val nextRankLast = board.pieces[index + 2 * moveDirection].type == PieceType.INVALID

                // moves
                if (board.pieces[index + moveDirection].type == PieceType.EMPTY) {
                    if (nextRankLast) {
                        for (pr in PROMOTION) {
                            possibleMoves.add(Move(index, index + moveDirection, promoteTo = pr))
                        }
                    } else {
                        possibleMoves.add(Move(index, index + moveDirection))
                        if (!piece.moved && board.pieces[index + 2 * moveDirection].type == PieceType.EMPTY) {
                            possibleMoves.add(Move(index, index + 2 * moveDirection))
                        }

                        if (board.lastMove.fromIndex != -1 && board.pieces[board.lastMove.toIndex].type == PieceType.PAWN && abs(board.lastMove.toIndex - board.lastMove.fromIndex) == 20 && abs(board.lastMove.toIndex - index) == 1) {
                            possibleMoves.add(Move(index, board.lastMove.toIndex + moveDirection, enPassant = true))
                        }
                    }
                }

                // captures
                for (offset in listOf(-1, 1)) {
                    val p = board.pieces[index + moveDirection + offset]
                    if (p.type != PieceType.EMPTY && p.type != PieceType.INVALID && p.white != piece.white) {
                        if (nextRankLast) {
                            for (pr in PROMOTION) {
                                possibleMoves.add(Move(index, index + moveDirection + offset, promoteTo = pr))
                            }
                        } else {
                            possibleMoves.add(Move(index, index + moveDirection + offset))
                        }
                    }
                }
            }

            PieceType.KING -> {
                for (offset in KING_MOVES) {
                    val p = board.pieces[index + offset]
                    if (p.type != PieceType.INVALID && (p.type == PieceType.EMPTY || p.white != piece.white)) {
                        possibleMoves.add(Move(index, index + offset))
                    }
                }

                if (!piece.moved && board.pieces[index - 1].type == PieceType.EMPTY && board.pieces[index - 2].type == PieceType.EMPTY && board.pieces[index - 3].let { it.type == PieceType.ROOK && !it.moved }) {
                    // castle short
                    possibleMoves.add(Move(index, index - 2, isCastle = true))
                }

                if (!piece.moved && board.pieces[index + 1].type == PieceType.EMPTY && board.pieces[index + 2].type == PieceType.EMPTY && board.pieces[index + 3].type == PieceType.EMPTY && board.pieces[index + 4].let { it.type == PieceType.ROOK && !it.moved }) {
                    // castle long
                    possibleMoves.add(Move(index, index + 2, isCastle = true))
                }
            }

            PieceType.KNIGHT -> {
                for (offset in KNIGHT_MOVES) {
                    val p = board.pieces[index + offset]
                    if (p.type != PieceType.INVALID && (p.type == PieceType.EMPTY || p.white != piece.white)) {
                        possibleMoves.add(Move(index, index + offset))
                    }
                }
            }

            PieceType.BISHOP -> {
                for (offset in BISHOP_MOVES) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }

            PieceType.ROOK -> {
                for (offset in ROOK_MOVES) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }

            PieceType.QUEEN -> {
                for (offset in QUEEN_MOVES) {
                    var p = board.pieces[index + offset]
                    var x = 1
                    while (p.type == PieceType.EMPTY) {
                        possibleMoves.add(Move(index, index + offset * x))
                        x += 1
                        p = board.pieces[index + offset * x]
                    }
                    // found non-empty square, check capture
                    if (p.type != PieceType.INVALID && p.white != piece.white) {
                        possibleMoves.add(Move(index, index + offset * x))
                    }
                }
            }
        }

        return possibleMoves
    }

    fun isMate(board: Board) : Boolean {
        val whiteHasMoves = board.getPieces(board.whiteToMove).any { findMovesSmart(board, it).isNotEmpty() }
        val blackHasCheck = board.getPieces(!board.whiteToMove).any { findMovesSimple(board, it).any { itt -> board.pieces[itt.toIndex].type == PieceType.KING } }

        return !whiteHasMoves && blackHasCheck
    }

    fun isStalemate(board: Board) : Boolean {
        val whiteHasMoves = board.getPieces(board.whiteToMove).any { findMovesSmart(board, it).isNotEmpty() }
        val blackHasCheck = board.getPieces(!board.whiteToMove).any { findMovesSimple(board, it).any { itt -> board.pieces[itt.toIndex].type == PieceType.KING } }

        return !whiteHasMoves && !blackHasCheck
    }

    fun isCheck(board: Board) : Boolean {
        return board.getPieces(!board.whiteToMove).any { findMovesSimple(board, it).any { itt -> board.pieces[itt.toIndex].type == PieceType.KING } }
    }

    /**
     * Finds moves,
     * then for each move, checks if the opponent can take a king,
     * and discards it if possible
     */
    fun findMovesSmart(board: Board, index: Int) : List<Move> {
        val cached = cache.getOrPut(board.hashCode()) { mutableMapOf() }[index]
        if (cached != null) {
            return cached
        }

        val p = board.pieces[index]

        val moves = findMovesSimple(board, index)
        val filtered = mutableListOf<Move>()

        for (move in moves) {
            if (move.isCastle) {
                val tmp1 = board.move(Move(-1, -1))
                val tmp2 = board.move(Move(move.fromIndex, move.fromIndex + (move.toIndex - move.fromIndex) / 2))
                val tmp3 = board.move(move)

                if (
                    tmp1.getPieces(!p.white).none { findMovesSimple(tmp1, it).any { itt -> tmp1.pieces[itt.toIndex].type == PieceType.KING } } &&
                    tmp2.getPieces(!p.white).none { findMovesSimple(tmp2, it).any { itt -> tmp2.pieces[itt.toIndex].type == PieceType.KING } } &&
                    tmp3.getPieces(!p.white).none { findMovesSimple(tmp3, it).any { itt -> tmp3.pieces[itt.toIndex].type == PieceType.KING } }
                ) {
                    filtered.add(move)
                }

            } else {
                val new = board.move(move)
                val kingSafe = new.getPieces(!p.white).none { findMovesSimple(new, it).any { itt -> new.pieces[itt.toIndex].type == PieceType.KING } }
                if (kingSafe) {
                    filtered.add(move)
                }
            }
        }

        return filtered.also { cache[board.hashCode()]!![index] = filtered }
    }
}