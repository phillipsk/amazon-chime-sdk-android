package com.amazonaws.services.chime.sdk.meetings.audiovideo.video.gl

import android.graphics.Matrix
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.security.InvalidParameterException

/**
 * [GlUtil] contains a broad collection of graphics constants and utilities that are used in multiple places within the SDK
 */
object GlUtil {
    /**
     * Vertex coordinates in Normalized Device Coordinates, i.e. (-1, -1) is bottom-left and (1, 1) is top-right.
     */
    val FULL_RECTANGLE_BUFFER =
        createFloatBuffer(
            floatArrayOf(
                -1.0f, -1.0f,  // Bottom left.
                1.0f, -1.0f,  // Bottom right.
                -1.0f, 1.0f,  // Top left.
                1.0f, 1.0f
            )
        )

    /**
     * Texture coordinates - (0, 0) is bottom-left and (1, 1) is top-right.
     */
    val FULL_RECTANGLE_TEXTURE_BUFFER =
        createFloatBuffer(
            floatArrayOf(
                0.0f, 0.0f,  // Bottom left.
                1.0f, 0.0f,  // Bottom right.
                0.0f, 1.0f,  // Top left.
                1.0f, 1.0f // Top right.
            )
        )

    private const val SIZEOF_FLOAT = 4

    /**
     * Checks to see if a OpenGLES error has been raised.
     */
    fun checkGlError(op: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$op: OpenGLES error $error")
        }
    }

    /**
     * Generate texture with standard parameters.
     */
    fun generateTexture(target: Int): Int {
        val textureArray = IntArray(1)
        GLES20.glGenTextures(1, textureArray, 0)
        val textureId = textureArray[0]
        GLES20.glBindTexture(target, textureId)
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(target, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        checkGlError("Failed to generate texture")
        return textureId
    }

    /**
     * Converts android.graphics.Matrix to a float[16] matrix array used by surfaces and OpenGL.
     */
    fun convertToGlTransformMatrix(matrix: Matrix): FloatArray {
        val values = FloatArray(9)
        matrix.getValues(values)
        return floatArrayOf(
            values[0 * 3 + 0], values[1 * 3 + 0], 0f, values[2 * 3 + 0],
            values[0 * 3 + 1], values[1 * 3 + 1], 0f, values[2 * 3 + 1],
            0f, 0f, 1f, 0f,
            values[0 * 3 + 2], values[1 * 3 + 2], 0f, values[2 * 3 + 2]
        )
    }

    /**
     * Converts float[16] matrix array used by surfaces and OpenGL to a android.graphics.Matrix.
     */
    fun convertToMatrix(transformMatrix: FloatArray): Matrix {
        val values = floatArrayOf(
            transformMatrix[0 * 4 + 0], transformMatrix[1 * 4 + 0], transformMatrix[3 * 4 + 0],
            transformMatrix[0 * 4 + 1], transformMatrix[1 * 4 + 1], transformMatrix[3 * 4 + 1],
            transformMatrix[0 * 4 + 3], transformMatrix[1 * 4 + 3], transformMatrix[3 * 4 + 3]
        )
        val matrix = Matrix()
        matrix.setValues(values)
        return matrix
    }

    /**
     * Creates a OpenGLES program from vertex and fragment shader sources.
     * Will throw exception on failures.
     */
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES20.glCreateProgram()
        checkGlError("Failed to create program")
        GLES20.glAttachShader(program, vertexShader)
        checkGlError("Failed to attach vertex shader")
        GLES20.glAttachShader(program, fragmentShader)
        checkGlError("Failed to attach pixel shader")
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val infoLog = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw InvalidParameterException("Could not link program; info log: $infoLog")
        }
        return program
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        checkGlError("Failed to create shader with type $shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val infoLog = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteProgram(shader)
            throw InvalidParameterException("Could not link program; info log: $infoLog")
        }
        return shader
    }

    private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coordinates into it.
        val byteBuffer = ByteBuffer.allocateDirect(coords.size * SIZEOF_FLOAT)
        byteBuffer.order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(coords)
        floatBuffer.position(0)
        return floatBuffer
    }

}