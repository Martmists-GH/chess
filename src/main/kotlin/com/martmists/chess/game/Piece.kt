package com.martmists.chess.game

class Piece {
    var type = PieceType.INVALID
    var white = false
    var moved = false

    fun empty() {
        type = PieceType.EMPTY
        white = false
        moved = false
    }

    fun from(p: Piece) {
        this.type = p.type
        this.white = p.white
        this.moved = p.moved
    }

    override fun hashCode(): Int {
        return white.int() + 2 * moved.int() + 4 * type.int()
    }
}

private fun Boolean.int() = if (this) 1 else 0
private fun PieceType.int() = PieceType.values().indexOf(this)
