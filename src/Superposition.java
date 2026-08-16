import java.util.List;

/**
 * Rigid-body (rotation + translation) superposition of one residue list onto
 * another, using Horn's closed-form quaternion method. Used only for the 3D
 * viewer - the domain-detection math itself is superposition-free by design.
 */
public class Superposition {

    public static final class Transform {
        private final double[][] rotation;
        private final double[] mobileCentroid;
        private final double[] referenceCentroid;

        Transform(double[][] rotation, double[] mobileCentroid, double[] referenceCentroid) {
            this.rotation = rotation;
            this.mobileCentroid = mobileCentroid;
            this.referenceCentroid = referenceCentroid;
        }

        public double[] apply(double x, double y, double z) {
            double dx = x - mobileCentroid[0];
            double dy = y - mobileCentroid[1];
            double dz = z - mobileCentroid[2];

            double rx = rotation[0][0] * dx + rotation[0][1] * dy + rotation[0][2] * dz;
            double ry = rotation[1][0] * dx + rotation[1][1] * dy + rotation[1][2] * dz;
            double rz = rotation[2][0] * dx + rotation[2][1] * dy + rotation[2][2] * dz;

            return new double[]{
                    rx + referenceCentroid[0],
                    ry + referenceCentroid[1],
                    rz + referenceCentroid[2]
            };
        }
    }

    /** Optimal rotation+translation so that transform.apply(mobile_i) best matches reference_i. */
    public static Transform fit(List<Residue> mobile, List<Residue> reference) {
        int n = Math.min(mobile.size(), reference.size());

        double[] mobileCentroid = centroid(mobile, n);
        double[] referenceCentroid = centroid(reference, n);

        double sxx = 0, sxy = 0, sxz = 0;
        double syx = 0, syy = 0, syz = 0;
        double szx = 0, szy = 0, szz = 0;

        for (int i = 0; i < n; i++) {
            Residue m = mobile.get(i);
            Residue r = reference.get(i);

            double mx = m.x - mobileCentroid[0];
            double my = m.y - mobileCentroid[1];
            double mz = m.z - mobileCentroid[2];

            double rx = r.x - referenceCentroid[0];
            double ry = r.y - referenceCentroid[1];
            double rz = r.z - referenceCentroid[2];

            sxx += mx * rx; sxy += mx * ry; sxz += mx * rz;
            syx += my * rx; syy += my * ry; syz += my * rz;
            szx += mz * rx; szy += mz * ry; szz += mz * rz;
        }

        double[][] n4 = {
                { sxx + syy + szz, syz - szy,        szx - sxz,        sxy - syx        },
                { syz - szy,       sxx - syy - szz,  sxy + syx,        szx + sxz        },
                { szx - sxz,       sxy + syx,       -sxx + syy - szz,  syz + szy        },
                { sxy - syx,       szx + sxz,        syz + szy,       -sxx - syy + szz  }
        };

        double[] q = dominantEigenvector(n4);
        double[][] rotation = quaternionToMatrix(q);

        return new Transform(rotation, mobileCentroid, referenceCentroid);
    }

    private static double[] centroid(List<Residue> residues, int n) {
        double x = 0, y = 0, z = 0;
        for (int i = 0; i < n; i++) {
            Residue r = residues.get(i);
            x += r.x; y += r.y; z += r.z;
        }
        return new double[]{x / n, y / n, z / n};
    }

    private static double[][] quaternionToMatrix(double[] q) {
        double w = q[0], x = q[1], y = q[2], z = q[3];
        double norm = Math.sqrt(w * w + x * x + y * y + z * z);

        if (norm < 1e-12) {
            return new double[][]{{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        }

        w /= norm; x /= norm; y /= norm; z /= norm;

        return new double[][]{
                {1 - 2 * (y * y + z * z), 2 * (x * y - z * w),     2 * (x * z + y * w)},
                {2 * (x * y + z * w),     1 - 2 * (x * x + z * z), 2 * (y * z - x * w)},
                {2 * (x * z - y * w),     2 * (y * z + x * w),     1 - 2 * (x * x + y * y)}
        };
    }

    /** Classic cyclic Jacobi eigenvalue algorithm; returns the eigenvector for the largest eigenvalue. */
    private static double[] dominantEigenvector(double[][] a) {
        int n = a.length;
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) m[i] = a[i].clone();

        double[][] v = new double[n][n];
        for (int i = 0; i < n; i++) v[i][i] = 1.0;

        for (int sweep = 0; sweep < 100; sweep++) {
            double off = 0;
            for (int p = 0; p < n; p++) {
                for (int q = p + 1; q < n; q++) {
                    off += m[p][q] * m[p][q];
                }
            }
            if (off < 1e-20) break;

            for (int p = 0; p < n - 1; p++) {
                for (int q = p + 1; q < n; q++) {
                    if (Math.abs(m[p][q]) < 1e-15) continue;

                    double theta = (m[q][q] - m[p][p]) / (2 * m[p][q]);
                    double t = theta == 0
                            ? 1
                            : Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));

                    double c = 1 / Math.sqrt(t * t + 1);
                    double s = t * c;

                    double mpp = m[p][p], mqq = m[q][q], mpq = m[p][q];

                    m[p][p] = c * c * mpp - 2 * s * c * mpq + s * s * mqq;
                    m[q][q] = s * s * mpp + 2 * s * c * mpq + c * c * mqq;
                    m[p][q] = 0;
                    m[q][p] = 0;

                    for (int i = 0; i < n; i++) {
                        if (i != p && i != q) {
                            double mip = m[i][p], miq = m[i][q];
                            m[i][p] = c * mip - s * miq;
                            m[p][i] = m[i][p];
                            m[i][q] = s * mip + c * miq;
                            m[q][i] = m[i][q];
                        }
                    }

                    for (int i = 0; i < n; i++) {
                        double vip = v[i][p], viq = v[i][q];
                        v[i][p] = c * vip - s * viq;
                        v[i][q] = s * vip + c * viq;
                    }
                }
            }
        }

        int best = 0;
        for (int i = 1; i < n; i++) {
            if (m[i][i] > m[best][best]) best = i;
        }

        double[] vec = new double[n];
        for (int i = 0; i < n; i++) vec[i] = v[i][best];
        return vec;
    }
}
