package com.martmists.chess

import com.martmists.chess.ai.*
import com.martmists.chess.game.*
import org.jetbrains.skija.*
import org.jetbrains.skija.svg.SVGDOM
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.glGetInteger
import org.lwjgl.system.MemoryUtil.NULL
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.math.min


class UI {
    var board = Board.standard()

    // Skija values
    private lateinit var context: DirectContext
    private lateinit var renderTarget: BackendRenderTarget
    private lateinit var surface: Surface

    private val window: Long

    // initial values
    private val initialWidth = 480f
    private val initialHeight = 480f

    private var width = initialWidth.toInt()
    private var height = initialHeight.toInt()

    // Seems to be needed?
    private var fbId = 0

    private var engine: Engine? =
         null  // player vs player
//         RandomMoveEngine()  // Random moves
//         MinMaxEngine(3)  // look 6 moves ahead
    private var selectedHover = -1
    private var selectedClicked = -1
    private val playAsWhite = true
    private var waitForEngine = !playAsWhite
    private var promotionMove = Move(-1, -1)
    private var future: CompletableFuture<Move>? = null

    init {
        glfwInit()
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        window = glfwCreateWindow(initialWidth.toInt(), initialHeight.toInt(), "Chess", NULL, NULL)
        glfwMakeContextCurrent(window)
        glfwSetWindowAspectRatio(window, 1, 1)
        glfwSwapInterval(1)
        glfwShowWindow(window)

        // set up callbacks
        glfwSetWindowSizeCallback(window) { w, width, height -> onResize(width, height) }
        glfwSetMouseButtonCallback(window) { w, button, action, mods -> onClick(button, action, mods) }
        glfwSetCursorPosCallback(window) { w, x, y -> onMouse(x, y) }
    }

    fun start() {
        resetBoard()

        GL.createCapabilities()
        context = DirectContext.makeGL()
        fbId = glGetInteger(0x8CA6) // GL_FRAMEBUFFER_BINDING

        renderTarget = BackendRenderTarget.makeGL(
            initialWidth.toInt(),
            initialHeight.toInt(),
            0,
            8,
            fbId,
            FramebufferFormat.GR_GL_RGBA8
        )

        surface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB()
        )

        // Render loop
        while (!glfwWindowShouldClose(window)) {
            surface.canvas.clear(0xFF769656.toInt())

            drawBoard()
            drawPieces()
            drawOverlay()

            context.flush()
            glfwSwapBuffers(window)

            if (waitForEngine) {
                doEngineMove()
            }
            glfwPollEvents()
        }

        surface.close()
        renderTarget.close()
        context.close()

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        glfwTerminate();
        glfwSetErrorCallback(null)?.free();
    }

    private fun drawOverlay() {
        val paint = Paint().setColor(0xFFFF0000.toInt()).setStroke(true)
        val lowest = min(width, height).toFloat()
        val squareSize = (lowest / 8)

        if (selectedClicked != -1) {
            val row = selectedClicked / 8
            val col = selectedClicked % 8

            var pieceRow = row
            var pieceCol = col
            if (playAsWhite) {
                pieceCol = 7 - pieceCol
                pieceRow = 7 - pieceRow
            }

            val boardIndex = 10 * (pieceRow + 2) + (pieceCol + 1)

            val moves = MoveGenerator.findMovesSmart(board.copy(), boardIndex)
            val paint2 = Paint().setColor(0xA0505050.toInt())
            moves.forEach {
                var _row = it.toIndex / 10 - 2
                var _col = it.toIndex % 10 - 1

                if (playAsWhite) {
                    _col = 7 - _col
                    _row = 7 - _row
                }

                surface.canvas.drawCircle((_col + .5f) * squareSize, (_row + .5f) * squareSize, squareSize / 4, paint2)
            }
            paint2.close()

            surface.canvas.drawRect(Rect(col * squareSize, row * squareSize, (col + 1) * squareSize, (row + 1) * squareSize), paint)

        } else {
            val column = selectedHover % 8
            val row = selectedHover / 8

            surface.canvas.drawRect(Rect(column * squareSize, row * squareSize, (column + 1) * squareSize, (row + 1) * squareSize), paint)
        }

        paint.close()

        val overlayPaint = Paint().setColor(0xEE333333.toInt())

        if (promotionMove.fromIndex != -1) {
            surface.canvas.drawRect(Rect(2.5f * squareSize, 2.5f * squareSize, 5.5f * squareSize, 5.5f * squareSize), overlayPaint)

            for ((index, piece) in listOf(
                Pair(27, PieceType.BISHOP),
                Pair(28, PieceType.KNIGHT),
                Pair(35, PieceType.ROOK),
                Pair(36, PieceType.QUEEN),
            )) {
                val filename = (if (promotionMove.fromIndex > 60) "white_" else "black_") + when (piece) {
                    PieceType.KNIGHT -> "knight"
                    PieceType.QUEEN -> "queen"
                    PieceType.BISHOP -> "bishop"
                    PieceType.ROOK -> "rook"
                    else -> throw IllegalStateException()
                }

                val svg = this::class.java.classLoader.getResource("$filename.svg")!!.toURI()
                val f = File(svg)
                val bytes = f.readBytes()
                val svgData = Data.makeFromBytes(bytes)
                val root = SVGDOM(svgData)

                val column = index % 8
                val row = index / 8

                surface.canvas.save()
                surface.canvas.translate(squareSize * column, squareSize * row)
                surface.canvas.scale(squareSize/45f, squareSize / 45f)

                root.render(surface.canvas)

                surface.canvas.restore()

                svgData.close()
                root.close()
            }
        }

        overlayPaint.close()
    }

    private fun drawPieces() {
        val lowest = min(width, height).toFloat()
        val squareSize = (lowest / 8)

        for (i in 0 until 120) {
            if (i in 20..100 && i % 10 in 1..8) {
                val piece = board.pieces[i]
                if (piece.type == PieceType.EMPTY) {
                    continue
                }

                var column = i % 10 - 1
                var row = i / 10 - 2

                if (playAsWhite) {
                    column = 7 - column
                    row = 7 - row
                }

                val canvas = surface.canvas

                val filename = (if (piece.white) "white_" else "black_") + when (piece.type) {
                    PieceType.PAWN -> "pawn"
                    PieceType.KNIGHT -> "knight"
                    PieceType.KING -> "king"
                    PieceType.QUEEN -> "queen"
                    PieceType.BISHOP -> "bishop"
                    PieceType.ROOK -> "rook"
                    else -> throw IllegalStateException()
                }
                val svg = this::class.java.classLoader.getResource("$filename.svg")!!.toURI()
                val f = File(svg)
                val bytes = f.readBytes()
                val svgData = Data.makeFromBytes(bytes)
                val root = SVGDOM(svgData)

                canvas.save()
                canvas.translate(squareSize * column, squareSize * row)
                canvas.scale(squareSize/45f, squareSize / 45f)

                root.render(canvas)

                canvas.restore()

                svgData.close()
                root.close()
            }
        }
    }

    private fun drawBoard() {
        val paint = Paint().setColor(0xFFEEEED2.toInt())
        val paint2 = Paint().setColor(0xFFF6F669.toInt())
        val paint3 = Paint().setColor(0xFFBACA2B.toInt())

        val lowest = min(width, height).toFloat()
        val squareSize = (lowest / 8)

        for (x in 0 until 64) {
            val column = x % 8
            val row = x / 8

            if (row % 2 == column % 2) {
                surface.canvas.drawRect(Rect(column * squareSize, row * squareSize, (column + 1) * squareSize, (row + 1) * squareSize), paint)
            }
        }

        if (board.lastMove.fromIndex != -1) {
            for (x in listOf(board.lastMove.fromIndex, board.lastMove.toIndex)) {
                var column = x % 10 - 1
                var row = x / 10 - 2

                if (playAsWhite) {
                    column = 7 - column
                    row = 7 - row
                }

                if (row % 2 == column % 2) {
                    surface.canvas.drawRect(Rect(column * squareSize, row * squareSize, (column + 1) * squareSize, (row + 1) * squareSize), paint2)
                } else {
                    surface.canvas.drawRect(Rect(column * squareSize, row * squareSize, (column + 1) * squareSize, (row + 1) * squareSize), paint3)
                }
            }
        }

        paint.close()
        paint2.close()
        paint3.close()
    }

    private fun onResize(width: Int, height: Int) {
        // Create new backend
        val newBackend = BackendRenderTarget.makeGL(
            width,
            height,
            0,
            8,
            fbId,
            FramebufferFormat.GR_GL_RGBA8
        )
        val newSurface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getSRGB()
        )

        val _renderTarget = renderTarget
        val _surface = surface
        renderTarget = newBackend
        surface = newSurface
        _renderTarget.close()
        _surface.close()

        this.width = width
        this.height = height
    }

    private fun onClick(button: Int, action: Int, mods: Int) {
        if (action == 0) {  // unpress
            if (button == GLFW_MOUSE_BUTTON_LEFT && !board.gameEnded && !waitForEngine) {
                if (promotionMove.fromIndex != -1) {
                    // check overlay
                    val type = when (selectedHover) {
                        27 -> PieceType.BISHOP
                        28 -> PieceType.KNIGHT
                        35 -> PieceType.ROOK
                        36 -> PieceType.QUEEN
                        else -> return
                    }

                    val selected = Move(promotionMove.fromIndex, promotionMove.toIndex, promoteTo = type)

                    // if promition done:
                    promotionMove = Move(-1, -1)

                    print(selected.notation(board) + " ")
                    board = board.move(selected)
                    board.checkState()

                    if (!board.gameEnded) {
                        if (engine != null) {
                            waitForEngine = true
                        }
                    }

                    if (board.gameEnded) {
                        checkGameEndReason()
                    }

                    return
                }

                if (selectedClicked == -1) {
                    var row = selectedHover / 8
                    var col = selectedHover % 8

                    if (playAsWhite) {
                        col = 7 - col
                        row = 7 - row
                    }

                    val boardIndex = 10 * (row + 2) + (col + 1)
                    val piece = board.pieces[boardIndex]

                    if (piece.type != PieceType.INVALID && piece.type != PieceType.EMPTY && piece.white == board.whiteToMove) {
                        selectedClicked = selectedHover
                    }

                } else {
                    var row = selectedClicked / 8
                    var col = selectedClicked % 8

                    if (playAsWhite) {
                        col = 7 - col
                        row = 7 - row
                    }

                    val boardIndex = 10 * (row + 2) + (col + 1)

                    var row2 = selectedHover / 8
                    var col2 = selectedHover % 8

                    if (playAsWhite) {
                        col2 = 7 - col2
                        row2 = 7 - row2
                    }

                    val boardIndexHover = 10 * (row2 + 2) + (col2 + 1)

                    val moves = MoveGenerator.findMovesSmart(board, boardIndex).filter { it.toIndex == boardIndexHover }

                    if (moves.size > 1) {
                        // promotion
                        promotionMove = moves.first()
                        selectedClicked = -1
                        return
                    }

                    val selected = moves.firstOrNull()

                    selected?.let {
                        print(it.notation(board) + " ")
                        board = board.move(it)
                        board.checkState()

                        if (!board.gameEnded) {
                            if (engine != null) {
                                waitForEngine = true
                            }
                        }

                        if (board.gameEnded) {
                            checkGameEndReason()
                        }
                    }
                    selectedClicked = -1
                }
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT && selectedClicked == -1) {
                // Reset game
                resetBoard()
            }
        }
    }

    private fun doEngineMove() {
        if (future == null) {
            val fut = CompletableFuture<Move>()
            future = fut

            fut.thenAccept { move ->
                print(move.notation(board) + " ")
                board = board.move(move)
                board.checkState()
                waitForEngine = false
                future = null
            }

            // Don't hang main thread
            thread(start = true, name = "Engine Thread", isDaemon = true) {
                val move = engine!!.genMove(board)
                fut.complete(move)
            }
        }

    }

    private fun resetBoard() {
        board = Board.standard()
        if (engine != null) {
            future?.cancel(true)

            waitForEngine = !playAsWhite
            if (!playAsWhite && engine != null) {
                engine!!.genMove(board)
            }
        }

    }

    private fun checkGameEndReason() {
        if (MoveGenerator.isMate(board)) {
            println("\n${if (board.whiteToMove) "Black" else "White"} won by checkmate")
        } else if (MoveGenerator.isStalemate(board)) {
            println("\nDraw by stalemate")
        } else {
            // check for insufficient material
            val whitePieces = board.getPieces(true)
            val blackPieces = board.getPieces(false)
            if (whitePieces.count { board.pieces[it].type == PieceType.KNIGHT || board.pieces[it].type == PieceType.BISHOP } <= 1 && whitePieces.count() <= 2 &&
                blackPieces.count { board.pieces[it].type == PieceType.KNIGHT || board.pieces[it].type == PieceType.BISHOP } <= 1 && blackPieces.count() <= 2
            ) {
                println("\nDraw by insufficient material")
            }
        }
    }

    private fun onMouse(x: Double, y: Double) {
        val lowest = min(width, height).toFloat()
        val squareSize = (lowest / 8)
        selectedHover = 8 * (y / squareSize).toInt() + (x / squareSize).toInt()
    }
}
