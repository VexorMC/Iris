/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Queues
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package com.mojang.blaze3d.vertex;

import com.google.common.collect.Queues;
import com.mojang.math.MatrixUtil;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public class PoseStack {
    private final Deque<Pose> poseStack = Util.make(Queues.newArrayDeque(), arrayDeque -> {
        Matrix4f matrix4f = new Matrix4f();
        Matrix3f matrix3f = new Matrix3f();
        arrayDeque.add(new Pose(matrix4f, matrix3f));
    });

    public void translate(double d, double d2, double d3) {
        this.translate((float)d, (float)d2, (float)d3);
    }

    public void translate(float f, float f2, float f3) {
        Pose pose = this.poseStack.getLast();
        pose.pose.translate(f, f2, f3);
    }

    public void translate(Vec3 vec3) {
        this.translate(vec3.x, vec3.y, vec3.z);
    }

    public void scale(float f, float f2, float f3) {
        Pose pose = this.poseStack.getLast();
        pose.pose.scale(f, f2, f3);
        if (Math.abs(f) == Math.abs(f2) && Math.abs(f2) == Math.abs(f3)) {
            if (f < 0.0f || f2 < 0.0f || f3 < 0.0f) {
                pose.normal.scale(Math.signum(f), Math.signum(f2), Math.signum(f3));
            }
            return;
        }
        pose.normal.scale(1.0f / f, 1.0f / f2, 1.0f / f3);
        pose.trustedNormals = false;
    }

    public void mulPose(Quaternionf quaternionf) {
        Pose pose = this.poseStack.getLast();
        pose.pose.rotate((Quaternionfc)quaternionf);
        pose.normal.rotate((Quaternionfc)quaternionf);
    }

    public void rotateAround(Quaternionf quaternionf, float f, float f2, float f3) {
        Pose pose = this.poseStack.getLast();
        pose.pose.rotateAround((Quaternionfc)quaternionf, f, f2, f3);
        pose.normal.rotate((Quaternionfc)quaternionf);
    }

    public void pushPose() {
        this.poseStack.addLast(new Pose(this.poseStack.getLast()));
    }

    public void popPose() {
        this.poseStack.removeLast();
    }

    public Pose last() {
        return this.poseStack.getLast();
    }

    public boolean clear() {
        return this.poseStack.size() == 1;
    }

    public void setIdentity() {
        Pose pose = this.poseStack.getLast();
        pose.pose.identity();
        pose.normal.identity();
        pose.trustedNormals = true;
    }

    public void mulPose(Matrix4f matrix4f) {
        Pose pose = this.poseStack.getLast();
        pose.pose.mul((Matrix4fc)matrix4f);
        if (!MatrixUtil.isPureTranslation(matrix4f)) {
            if (MatrixUtil.isOrthonormal(matrix4f)) {
                pose.normal.mul((Matrix3fc)new Matrix3f((Matrix4fc)matrix4f));
            } else {
                pose.computeNormalMatrix();
            }
        }
    }

    public static final class Pose {
        final Matrix4f pose;
        final Matrix3f normal;
        boolean trustedNormals = true;

        Pose(Matrix4f matrix4f, Matrix3f matrix3f) {
            this.pose = matrix4f;
            this.normal = matrix3f;
        }

        Pose(Pose pose) {
            this.pose = new Matrix4f((Matrix4fc)pose.pose);
            this.normal = new Matrix3f((Matrix3fc)pose.normal);
            this.trustedNormals = pose.trustedNormals;
        }

        void computeNormalMatrix() {
            this.normal.set((Matrix4fc)this.pose).invert().transpose();
            this.trustedNormals = false;
        }

        public Matrix4f pose() {
            return this.pose;
        }

        public Matrix3f normal() {
            return this.normal;
        }

        public Vector3f transformNormal(Vector3f vector3f, Vector3f vector3f2) {
            return this.transformNormal(vector3f.x, vector3f.y, vector3f.z, vector3f2);
        }

        public Vector3f transformNormal(float f, float f2, float f3, Vector3f vector3f) {
            Vector3f vector3f2 = this.normal.transform(f, f2, f3, vector3f);
            return this.trustedNormals ? vector3f2 : vector3f2.normalize();
        }

        public Pose copy() {
            return new Pose(this);
        }
    }
}

