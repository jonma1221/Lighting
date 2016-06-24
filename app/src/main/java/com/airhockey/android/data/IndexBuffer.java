package com.airhockey.android.data;

import com.airhockey.android.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static android.opengl.GLES20.GL_ELEMENT_ARRAY_BUFFER;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_STATIC_DRAW;
import static android.opengl.GLES20.glBindBuffer;
import static android.opengl.GLES20.glBufferData;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenBuffers;
import static android.opengl.GLES20.glVertexAttribPointer;

/**
 * Created by pixuredlinux3 on 6/20/16.
 */
public class IndexBuffer {
    private final int bufferId;

    public IndexBuffer(short[] vertexData) {

        // Allocate a buffer.
        final int buffers[] = new int[1];
        glGenBuffers(buffers.length, buffers, 0);
        if (buffers[0] == 0) {
            throw new RuntimeException("Could not create a new vertex buffer object.");
        }
        bufferId = buffers[0];

        // Bind to the buffer.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0]);

        // Transfer data to native memory.
        ShortBuffer vertexArray = ByteBuffer
                .allocateDirect(vertexData.length * Constants.BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(vertexData);
        vertexArray.position(0);

        // Transfer data from native memory to the GPU buffer.
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertexArray.capacity() * Constants.BYTES_PER_SHORT,
                vertexArray, GL_STATIC_DRAW);

        //  Unbind from the buffer when we're done with it.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public void setVertexAttribPointer(int dataOffset, int attributeLocation,
                                       int componentCount, int stride) {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER , bufferId);
        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT,
                false, stride, dataOffset);
        glEnableVertexAttribArray(attributeLocation);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    }

    public int getBufferId() {
        return bufferId;
    }
}
