//https://github.com/psyclone20/k-means-clustering

/*
MIT License

Copyright (c) 2017 Adnan Ansari

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package org.walley.guidepost;


import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;


public class KMeans2
{

  static String TAG = "GP-KM2";

  double[][] points;
  int curr_row;
  int records;
  int maxIterations;
  int clusters;
  double[][] means;
  ArrayList<Integer>[] oldClusters;
  ArrayList<Integer>[] newClusters;

  public KMeans2(int max)
  {
    curr_row = 0;
    // Open the file just to count the number of records
    records = max;

    // Open file again to read the records
    points = new double[records][2];
  }

  public void create_clusters(int numclusters, int numiterations)
  {

    // Sort the points based on X-coordinate values
    sortPointsByX();

    maxIterations = numiterations;
    clusters = numclusters;

    // Calculate initial means
    means = new double[clusters][2];
    for (int i = 0; i < means.length; i++) {
      means[i][0] = points[(int) (Math.floor(
              (records * 1.0 / clusters) / 2) + i * records / clusters)][0];
      means[i][1] = points[(int) (Math.floor(
              (records * 1.0 / clusters) / 2) + i * records / clusters)][1];
    }

    // Create skeletons for clusters
    oldClusters = new ArrayList[clusters];
    newClusters = new ArrayList[clusters];

    for (int i = 0; i < clusters; i++) {
      oldClusters[i] = new ArrayList<Integer>();
      newClusters[i] = new ArrayList<Integer>();
    }

    // Make the initial clusters
    formClusters(oldClusters);
    int iterations = 0;

    displayOutput();

    // Showtime
    while (true) {
      updateMeans(oldClusters);
      formClusters(newClusters);

      iterations++;

      if (iterations > maxIterations || checkEquality())
        break;
      else
        resetClusters();
    }

    displayOutput();
    Log.i(TAG, "Iterations taken = " + iterations);
  }


  public void add(double x, double y)
  {
    points[curr_row][0] = x;
    points[curr_row++][1] = y;
  }

  private void sortPointsByX()
  {
    double[] temp;

    // Bubble Sort
    for (int i = 0; i < points.length; i++)
      for (int j = 1; j < (points.length - i); j++)
        if (points[j - 1][0] > points[j][0]) {
          temp = points[j - 1];
          points[j - 1] = points[j];
          points[j] = temp;
        }
  }

  private void updateMeans(ArrayList<Integer>[] clusterList)
  {
    double totalX = 0;
    double totalY = 0;
    for (int i = 0; i < clusterList.length; i++) {
      totalX = 0;
      totalY = 0;
      for (int index : clusterList[i]) {
        totalX += points[index][0];
        totalY += points[index][1];
      }
      means[i][0] = totalX / clusterList[i].size();
      means[i][1] = totalY / clusterList[i].size();
    }
  }

  private void formClusters(ArrayList<Integer>[] clusterList)
  {
    double distance[] = new double[means.length];
    double minDistance = 999999999;
    int minIndex = 0;

    for (int i = 0; i < points.length; i++) {
      minDistance = 999999999;
      for (int j = 0; j < means.length; j++) {
        distance[j] = Math.sqrt(
                Math.pow((points[i][0] - means[j][0]), 2) + Math.pow(
                        (points[i][1] - means[j][1]),
                        2
                                                                    ));
        if (distance[j] < minDistance) {
          minDistance = distance[j];
          minIndex = j;
        }
      }
      clusterList[minIndex].add(i);
    }
  }

  private boolean checkEquality()
  {
    for (int i = 0; i < oldClusters.length; i++) {
      // Check only lengths first
      if (oldClusters[i].size() != newClusters[i].size())
        return false;

      // Check individual values if lengths are equal
      for (int j = 0; j < oldClusters[i].size(); j++)
        if (oldClusters[i].get(j) != newClusters[i].get(j))
          return false;
    }

    return true;
  }

  private void resetClusters()
  {
    for (int i = 0; i < newClusters.length; i++) {
      // Copy newClusters to oldClusters
      oldClusters[i].clear();
      for (int index : newClusters[i])
        oldClusters[i].add(index);

      // Clear newClusters
      newClusters[i].clear();
    }
  }

  public double[][] get_centroids()
  {
    return means;
  }

  public int numclusters()
  {
    return oldClusters.length;
  }

  public void displayOutput()
  {

    for (int i = 0; i < oldClusters.length; i++) {
      Log.i(TAG, "cluster: " + i + " " + oldClusters[i].size());

      String clusterOutput = "";
      for (int index : oldClusters[i]) {
        clusterOutput += "(" + points[index][0] + ", " + points[index][1] + "), ";
      }

      Log.i(TAG, clusterOutput);
    }

    for (int i = 0; i < newClusters.length; i++) {
      Log.i(TAG, "new cluster: " + i + " " + newClusters[i].size());

      String clusterOutput = "";
      for (int index : newClusters[i]) {
        clusterOutput += "(" + points[index][0] + ", " + points[index][1] + "), ";
      }

      Log.i(TAG, clusterOutput);
    }

    for (int i = 0; i < means.length; i++) {
      Log.i(TAG, i + " " + means[i][0] + " " + means[i][1]);
    }
  }
}
