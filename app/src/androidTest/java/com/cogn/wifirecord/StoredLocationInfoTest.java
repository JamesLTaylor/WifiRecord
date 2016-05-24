package com.cogn.wifirecord;

import android.test.ActivityTestCase;

import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class StoredLocationInfoTest extends ActivityTestCase {
    public void testLoad()
    {
        ConnectionPoints connectionPoints = new ConnectionPoints();
        connectionPoints.add(0, 530, 320, 1,570, 660);
        connectionPoints.add(0, 1020, 425, 1,1100, 690);

        InputStream summaryResourceStream = getInstrumentation().getContext().getResources().openRawResource(R.raw.greenstone_macs);
        InputStream summaryResourceStream2 = getInstrumentation().getContext().getResources().openRawResource(R.raw.greenstone_macs);
        StoredLocationInfo list = new StoredLocationInfo(connectionPoints, summaryResourceStream);

        HashMap<Integer,List<Float>> testReading = new HashMap<>();
        testReading.put(4, Arrays.asList(0.925f, -64.8648648648648f, 2.95151964672128f));
        testReading.put(5, Arrays.asList(0.925f, -64.5675675675675f, 2.48824849437489f));
        testReading.put(6, Arrays.asList(0.925f, -64.6756756756756f, 3.10234483292578f));
        testReading.put(7, Arrays.asList(0.925f, -51.8378378378378f, 3.15811707084504f));
        testReading.put(8, Arrays.asList(0.925f, -51.9189189189189f, 3.3156335530771f));
        testReading.put(9, Arrays.asList(0.925f, -52.1621621621621f, 3.18368742110858f));
        testReading.put(10, Arrays.asList(0.925f, -71.9189189189189f, 2.86995977350449f));
        testReading.put(11, Arrays.asList(0.725f, -82.2413793103448f, 3.65452173779083f));
        testReading.put(15, Arrays.asList(0.7f, -70.3928571428571f, 1.29115910589465f));
        testReading.put(16, Arrays.asList(0.7f, -70.2857142857142f, 1.22057196361679f));
        testReading.put(17, Arrays.asList(0.725f, -84.551724137931f, 3.35884018258921f));
        testReading.put(18, Arrays.asList(0.7f, -84.7857142857142f, 2.79485679235912f));
        testReading.put(23, Arrays.asList(0.15f, -81f, 0f));
        testReading.put(24, Arrays.asList(0.075f, -89f, 0f));
        testReading.put(25, Arrays.asList(0.375f, -80.4666666666666f, 0.884433277428106f));
        testReading.put(26, Arrays.asList(0.925f, -65.9729729729729f, 4.74476375105479f));
        testReading.put(27, Arrays.asList(0.925f, -79f, 6.0179910448767f));
        testReading.put(28, Arrays.asList(0.925f, -68f, 3.01348321400353f));
        testReading.put(29, Arrays.asList(0.075f, -89f, 0f));
        testReading.put(30, Arrays.asList(0.35f, -91f, 0f));
        testReading.put(32, Arrays.asList(0.2f, -82f, 3.46410161513775f));
        testReading.put(33, Arrays.asList(0.35f, -89.8571428571428f, 1.12485826771597f));
        testReading.put(34, Arrays.asList(0.925f, -67.3513513513513f, 2.46287259457178f));
        testReading.put(35, Arrays.asList(0.775f, -77.3225806451612f, 2.24882903973333f));
        testReading.put(36, Arrays.asList(0.925f, -62.5945945945945f, 2.38757435949813f));
        testReading.put(37, Arrays.asList(0.675f, -89.4074074074074f, 0.491351820793392f));
        testReading.put(38, Arrays.asList(0.925f, -79.4054054054054f, 6.7160938593455f));
        testReading.put(39, Arrays.asList(0.925f, -76.5945945945945f, 2.36482625451106f));
        testReading.put(40, Arrays.asList(0.925f, -81.8648648648648f, 5.50754844251081f));
        testReading.put(41, Arrays.asList(0.925f, -81.9729729729729f, 5.44499019176882f));
        testReading.put(43, Arrays.asList(0.925f, -68.054054054054f, 3.28753539449404f));
        testReading.put(44, Arrays.asList(0.925f, -62.4594594594594f, 2.51046022048283f));
        testReading.put(45, Arrays.asList(0.925f, -62.4054054054054f, 2.22345554607601f));
        testReading.put(46, Arrays.asList(0.65f, -90.1538461538461f, 1.06309807392963f));
        testReading.put(47, Arrays.asList(0.925f, -76.7837837837837f, 2.54829179812392f));
        testReading.put(48, Arrays.asList(0.925f, -76.6756756756756f, 2.23067138183387f));
        testReading.put(49, Arrays.asList(0.925f, -82.3783783783783f, 5.05296996611412f));
        testReading.put(51, Arrays.asList(0.6f, -89.125f, 1.48077963699307f));
        testReading.put(52, Arrays.asList(0.725f, -89.8620689655172f, 1.50227730419254f));
        testReading.put(53, Arrays.asList(0.6f, -89.75f, 1.56124949959959f));
        testReading.put(54, Arrays.asList(0.925f, -69.8378378378378f, 3.78127329843892f));
        testReading.put(56, Arrays.asList(0.825f, -76.3333333333333f, 3.09120616516523f));
        testReading.put(57, Arrays.asList(0.925f, -72.1621621621621f, 3.95592961373664f));
        testReading.put(58, Arrays.asList(0.925f, -66.081081081081f, 3.19947950478417f));
        testReading.put(59, Arrays.asList(0.575f, -74.3478260869565f, 3.42127227354886f));
        testReading.put(60, Arrays.asList(0.925f, -69.3513513513513f, 1.64754061125709f));
        testReading.put(61, Arrays.asList(0.925f, -78.8918918918918f, 4.25398880406018f));
        testReading.put(62, Arrays.asList(0.925f, -75.027027027027f, 3.49119732026124f));
        testReading.put(63, Arrays.asList(0.85f, -74.3235294117647f, 2.45812683628257f));
        testReading.put(64, Arrays.asList(0.85f, -74.6764705882352f, 2.23238975401716f));
        testReading.put(67, Arrays.asList(0.925f, -72.1351351351351f, 4.0347287136249f));
        testReading.put(68, Arrays.asList(0.775f, -77.6451612903225f, 1.99270260793296f));
        testReading.put(69, Arrays.asList(0.925f, -69.4594594594594f, 1.44436640171755f));
        testReading.put(70, Arrays.asList(0.925f, -67f, 4.16549533085218f));
        testReading.put(71, Arrays.asList(0.925f, -70.7297297297297f, 4.98998266068941f));
        testReading.put(74, Arrays.asList(0.45f, -74.0555555555555f, 4.14289515277825f));
        testReading.put(75, Arrays.asList(0.925f, -78.4324324324324f, 4.34051024862372f));
        testReading.put(79, Arrays.asList(0.925f, -78.8918918918918f, 2.76839092482202f));
        testReading.put(80, Arrays.asList(0.925f, -70.4594594594594f, 4.41492918491519f));
        testReading.put(81, Arrays.asList(0.925f, -66.5945945945945f, 2.03296427773962f));
        testReading.put(82, Arrays.asList(0.925f, -67.1621621621621f, 2.53103454452825f));
        testReading.put(83, Arrays.asList(0.925f, -66.7027027027027f, 3.10940043638262f));
        testReading.put(84, Arrays.asList(0.925f, -69.1891891891891f, 1.72168738471707f));
        testReading.put(85, Arrays.asList(0.325f, -78.1538461538461f, 2.2134607030674f));
        testReading.put(86, Arrays.asList(0.925f, -78.9459459459459f, 4.05999564602313f));
        testReading.put(88, Arrays.asList(0.925f, -85.4594594594594f, 2.62621497848544f));
        testReading.put(89, Arrays.asList(0.65f, -88.5384615384615f, 2.35716206862224f));
        testReading.put(90, Arrays.asList(0.875f, -78.4857142857142f, 3.21932937622931f));
        testReading.put(93, Arrays.asList(0.225f, -80f, 0f));
        testReading.put(94, Arrays.asList(0.925f, -84.9189189189189f, 3.5669532640645f));
        testReading.put(96, Arrays.asList(0.9f, -78.6666666666666f, 2.77888866675551f));
        testReading.put(97, Arrays.asList(0.925f, -76.4324324324324f, 4.48746326065238f));
        testReading.put(98, Arrays.asList(0.925f, -65.3783783783783f, 2.4423222373151f));
        testReading.put(99, Arrays.asList(0.925f, -80.7297297297297f, 2.12639332570496f));
        testReading.put(100, Arrays.asList(0.725f, -87.4827586206896f, 1.22109870411212f));
        testReading.put(101, Arrays.asList(0.6f, -80.875f, 2.84769643747362f));
        testReading.put(102, Arrays.asList(0.925f, -72.7027027027027f, 2.11640777842723f));
        testReading.put(103, Arrays.asList(0.325f, -76.6923076923076f, 0.721602424588219f));
        testReading.put(104, Arrays.asList(0.925f, -71.7567567567567f, 2.14860615290289f));
        testReading.put(105, Arrays.asList(0.625f, -88.8799999999999f, 2.83294899354012f));
        testReading.put(106, Arrays.asList(0.925f, -66.2972972972972f, 2.88392436142578f));
        testReading.put(107, Arrays.asList(0.3f, -81f, 0f));
        testReading.put(108, Arrays.asList(0.55f, -87f, 1f));
        testReading.put(109, Arrays.asList(0.925f, -65.5135135135135f, 2.96485377809649f));
        testReading.put(123, Arrays.asList(0.925f, -81f, 2.38236766173394f));
        testReading.put(126, Arrays.asList(0.675f, -80.7777777777777f, 2.46956786343254f));
        testReading.put(127, Arrays.asList(0.275f, -80f, 0f));
        testReading.put(129, Arrays.asList(0.275f, -79f, 0f));
        testReading.put(130, Arrays.asList(0.525f, -76.7142857142857f, 1.38505138783323f));
        testReading.put(135, Arrays.asList(0.35f, -92f, 0f));
        testReading.put(139, Arrays.asList(0.525f, -88.7619047619047f, 3.79102652142764f));
        testReading.put(144, Arrays.asList(0.35f, -91.9285714285714f, 0.593330275922719f));
        testReading.put(146, Arrays.asList(0.375f, -92.3333333333333f, 0.942809041582063f));
        testReading.put(211, Arrays.asList(0.35f, -77.5714285714285f, 0.903507902905251f));
        testReading.put(212, Arrays.asList(0.65f, -70.6153846153846f, 4.32414440694006f));
        testReading.put(213, Arrays.asList(0.925f, -75.2432432432432f, 3.30769296105556f));
        testReading.put(216, Arrays.asList(0.475f, -67.8421052631578f, 3.08243167670448f));
        testReading.put(218, Arrays.asList(0.425f, -82.5294117647058f,  2.0034572195207532f));

        list.updateScores(testReading);
    }
}
