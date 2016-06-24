package com.airhockey.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.opengl.GLSurfaceView.Renderer;

import com.airhockey.android.objects.Heightmap;
import com.airhockey.android.objects.ParticleShooter;
import com.airhockey.android.objects.ParticleSystem;
import com.airhockey.android.objects.Skybox;
import com.airhockey.android.programs.HeightmapShaderProgram;
import com.airhockey.android.programs.ParticleShaderProgram;
import com.airhockey.android.programs.SkyboxShaderProgram;
import com.airhockey.android.util.Geometry;
import com.airhockey.android.util.Geometry.Point;
import com.airhockey.android.util.Geometry.Vector;
import com.airhockey.android.util.MatrixHelper;
import com.airhockey.android.util.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.*;
import static android.opengl.Matrix.*;

/**
 * Created by Jonathan on 6/14/2016.
 */
public class ParticlesRenderer implements Renderer {
    private final Context context;
    /*private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];*/
    //private final float[] viewProjectionMatrix = new float[16];

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewMatrixForSkybox = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] tempMatrix = new float[16];
    private final float[] modelViewProjectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];
    private final float[] it_modelViewMatrix = new float[16];

    // Particle fields
    private ParticleShaderProgram particleProgram;
    private ParticleSystem particleSystem;
    private ParticleShooter redParticleShooter;
    private ParticleShooter greenParticleShooter;
    private ParticleShooter blueParticleShooter;
    long globalStartTime;
    final float angleVarianceInDegrees = 5f;
    final float speedVariance = 1f;
    private int particleTexture;

    // Skybox fields
    private SkyboxShaderProgram skyboxProgram;
    private Skybox skybox;
    private int skyboxTexture;

    // Heightmap fields
    private HeightmapShaderProgram heightmapProgram;
    private Heightmap heightmap;

    //private final Vector vectorToLight = new Vector(0.30f, 0.35f, -0.89f).normalize();
    final float[] vectorToLight = {0.30f, 0.35f, -0.89f, 0f};

    private final float[] pointLightPositions = new float[]{
            -1f, 1f, 0f, 1f,
            0f, 1f, 0f, 1f,
            1f, 1f, 0f, 1f
    };
    private final float[] pointLightColors = new float[]
            {1.00f, 0.20f, 0.02f,
                    0.02f, 0.25f, 0.02f,
                    0.02f, 0.20f, 1.00f};

    public ParticlesRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_DEPTH_TEST);

        // Particle initialization
        particleProgram = new ParticleShaderProgram(context);
        particleSystem = new ParticleSystem(10000);
        globalStartTime = System.nanoTime();
        final Geometry.Vector particleDirection = new Vector(0f, 0.5f, 0f);
        redParticleShooter = new ParticleShooter(
                new Point(-1f, 0f, 0f),
                particleDirection,
                Color.rgb(255, 50, 5),
                angleVarianceInDegrees,
                speedVariance );
        greenParticleShooter = new ParticleShooter(
                new Point(0f, 0f, 0f),
                particleDirection,
                Color.rgb(25, 255, 25),
                angleVarianceInDegrees,
                speedVariance );
        blueParticleShooter = new ParticleShooter(
                new Point(1f, 0f, 0f),
                particleDirection,
                Color.rgb(5, 50, 255),
                angleVarianceInDegrees,
                speedVariance );

        // load particleTexture for particles
        particleTexture = TextureHelper.loadTexture(context, R.drawable.particle_texture);

        // skybox initialization
        skyboxProgram = new SkyboxShaderProgram(context);
        skybox = new Skybox();
        /*skyboxTexture = TextureHelper.loadCubeMap(context,
                new int[] { R.drawable.left, R.drawable.right,
                        R.drawable.bottom, R.drawable.top,
                        R.drawable.front, R.drawable.back});*/
        skyboxTexture = TextureHelper.loadCubeMap(context,
                new int[] { R.drawable.night_left, R.drawable.night_right,
                        R.drawable.night_bottom, R.drawable.night_top,
                        R.drawable.night_front, R.drawable.night_back});
        // heightmap initialization
        heightmapProgram = new HeightmapShaderProgram(context);
        heightmap = new Heightmap(((BitmapDrawable)context.getResources()
                .getDrawable(R.drawable.heightmap)).getBitmap());


    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width
                / (float) height, 1f, 100f);
        updateViewMatrices();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //glClear(GL_COLOR_BUFFER_BIT);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        drawHeightmap();
        drawSkybox();
        drawParticles();
    }

    private void drawParticles() {
        float currentTime = (System.nanoTime() - globalStartTime) / 1000000000f;

        redParticleShooter.addParticles(particleSystem, currentTime, 5);
        greenParticleShooter.addParticles(particleSystem, currentTime, 5);
        blueParticleShooter.addParticles(particleSystem, currentTime, 5);

        /*setIdentityM(viewMatrix, 0);
        translateM(viewMatrix, 0, 0f, -1.5f, -5f);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);*/
        setIdentityM(modelMatrix, 0);
        updateMvpMatrix();

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);

        particleProgram.useProgram();
        //particleProgram.setUniforms(viewProjectionMatrix, currentTime, particleTexture);
        particleProgram.setUniforms(modelViewProjectionMatrix, currentTime, particleTexture);
        particleSystem.bindData(particleProgram);
        glDepthMask(false);
        particleSystem.draw();
        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    public void drawSkybox(){
        /*setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);*/
        setIdentityM(modelMatrix, 0);
        updateMvpMatrixForSkybox();

        skyboxProgram.useProgram();
        //skyboxProgram.setUniforms(viewProjectionMatrix, skyboxTexture);
        skyboxProgram.setUniforms(modelViewProjectionMatrix, skyboxTexture);
        skybox.bindData(skyboxProgram);
        glDepthFunc(GL_LEQUAL);
        skybox.draw();
        glDepthFunc(GL_LESS);
    }

    private void drawHeightmap() {
        setIdentityM(modelMatrix, 0);
        scaleM(modelMatrix, 0, 100f, 10f, 100f);
        updateMvpMatrix();
        heightmapProgram.useProgram();
        //heightmapProgram.setUniforms(modelViewProjectionMatrix, vectorToLight);
        // Put the light positions into eye space.
        final float[] vectorToLightInEyeSpace = new float[4];
        final float[] pointPositionsInEyeSpace = new float[12];
        multiplyMV(vectorToLightInEyeSpace, 0, viewMatrix, 0, vectorToLight, 0);
        multiplyMV(pointPositionsInEyeSpace, 0, viewMatrix, 0, pointLightPositions, 0);
        multiplyMV(pointPositionsInEyeSpace, 4, viewMatrix, 0, pointLightPositions, 4);
        multiplyMV(pointPositionsInEyeSpace, 8, viewMatrix, 0, pointLightPositions, 8);
        heightmapProgram.setUniforms(modelViewMatrix, it_modelViewMatrix,
                modelViewProjectionMatrix, vectorToLightInEyeSpace,
                pointPositionsInEyeSpace, pointLightColors);
        heightmap.bindData(heightmapProgram);
        heightmap.draw();



    }

    private void updateViewMatrices() {
        setIdentityM(viewMatrix, 0);
        rotateM(viewMatrix, 0, -yRotation, 1f, 0f, 0f);
        rotateM(viewMatrix, 0, -xRotation, 0f, 1f, 0f);
        System.arraycopy(viewMatrix, 0, viewMatrixForSkybox, 0, viewMatrix.length);

        // We want the translation to apply to the regular view matrix, and not
        // the skybox.
        translateM(viewMatrix, 0, 0, -1.5f, -5f);
    }

    private void updateMvpMatrix() {
        multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        invertM(tempMatrix, 0, modelViewMatrix, 0);
        transposeM(it_modelViewMatrix, 0, tempMatrix, 0);
        multiplyMM(
                modelViewProjectionMatrix, 0,
                projectionMatrix, 0,
                modelViewMatrix, 0);
    }

    private void updateMvpMatrixForSkybox() {
        multiplyMM(tempMatrix, 0, viewMatrixForSkybox, 0, modelMatrix, 0);
        multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, tempMatrix, 0);
    }

    private float xRotation, yRotation;
    public void handleTouchDrag(float deltaX, float deltaY) {
        xRotation += deltaX / 16f;
        yRotation += deltaY / 16f;
        if (yRotation < -90) {
            yRotation = -90;
        } else if (yRotation > 90) {
            yRotation = 90;
        }

        updateViewMatrices();
    }

}
