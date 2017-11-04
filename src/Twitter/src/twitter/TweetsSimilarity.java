/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package twitter;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import java.io.FileWriter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @author Eirini Mitsopoulou
 */

public class TweetsSimilarity {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        HashMap<String, Integer> hashtags = new HashMap<>();
        ArrayList<String> mostUsedHashtags = new ArrayList<>();
        LinkedHashMap<String, ArrayList<String>> hashmap = new LinkedHashMap<>();
        LinkedHashMap<String, String> hashmap2 = new LinkedHashMap<>();
        LinkedHashMap<String, ArrayList<String>> hashmap3 = new LinkedHashMap<>();
        LinkedHashMap<String, ArrayList<String>> hashmap4 = new LinkedHashMap<>();

        try {
            // To connect to mongodb server
            MongoClient mongoClient = new MongoClient("localhost", 27017);

            // Now connect to your databases
            DB db = mongoClient.getDB("tweetDB2");
            System.out.println("Connect to database successfully");

            DBCollection coll = db.getCollection("test2");

            DBCursor cursor = coll.find();
            int size = 0;
            while (cursor.hasNext()) {// find entities used by a large percentage of users

                BasicDBObject curr = (BasicDBObject) cursor.next();
                String s = curr.get("tweet_text").toString();
                String[] words = s.split("\\s+");
                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].toLowerCase();
                    if (words[i].charAt(0) == '#' && words[i].length() > 1) {
                        if (words[i].matches(".*\\p{Punct}")) {
                            words[i] = "#" + words[i].replaceAll("\\p{P}", "");
                        }
                        if (!hashtags.containsKey(words[i])) {
                            hashtags.put(words[i], 1);
                        } else {
                            hashtags.put(words[i], hashtags.get(words[i]) + 1);
                        }
                        size++;
                    }
                }
            }

            for (HashMap.Entry<String, Integer> entry : hashtags.entrySet()) {
                //System.out.println(entry.getKey()+" : "+entry.getValue());
                int percent = (int) (size * (50.0f / 100.0f));  // find entites used up to 50% by total users
                if (entry.getValue() > percent) {
                    mostUsedHashtags.add(entry.getKey());
                }
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }

        try {

            // To connect to mongodb server
            MongoClient mongoClient = new MongoClient("localhost", 27017);

            // Now connect to your databases
            DB db = mongoClient.getDB("tweetDB2");
            System.out.println("Connect to database successfully");
            DB db2 = mongoClient.getDB("tweetDB3");

            DBCollection coll = db.getCollection("test2");
            System.out.println("Collection selected successfully");
            DBCollection coll2 = db2.getCollection("test2");

            //tweets text analysis saved in tweetDB3 DB and test2 collection
            DBCursor cursor = coll.find();
            boolean bool1 = false, bool2 = false, bool3 = false, bool4 = false;
            while (cursor.hasNext()) { // for each user's tweet
                bool1 = false;
                bool2 = false;
                bool3 = false;
                bool4 = false;
                BasicDBObject curr = (BasicDBObject) cursor.next();
                String user = curr.get("user_name").toString();
                System.out.println(user);
                String s = curr.get("tweet_text").toString();
                System.out.println(s);
                String[] words = s.split("\\s+");
                if (words[0].equals("RT")) { // find retweeted tweets
                    String tweet[] = s.split(" ", 2);
                    String tweet2[] = tweet[1].split(" ", 2);
                    System.out.println(tweet2[1]);
                    BasicDBObject document = new BasicDBObject();
                    document.put("user", user);
                    document.put("timestamp", new Date());
                    document.put("retweeted_tweet", tweet2[1]);
                    coll2.insert(document);
                    bool1 = true;
                }
                for (int i = 0; i < words.length; i++) {
                    if (words[i].charAt(0) == '#' && words[i].length() > 1) {// find hashtags
                        words[i] = words[i].toLowerCase();
                        if (words[i].matches(".*\\p{Punct}")) {
                            words[i] = "#" + words[i].replaceAll("\\p{P}", "");
                        }
                        System.out.println(words[i]);
                        if (!mostUsedHashtags.contains(words[i])) {
                            BasicDBObject document = new BasicDBObject();
                            document.put("user", user);
                            document.put("timestamp", new Date());
                            document.put("hashtag", words[i]);
                            coll2.insert(document);
                            bool2 = true;
                        }
                    }
                    if (words[i].contains("@") && words[i].length() > 1) { // find mentions
                        if (words[i].charAt(words[i].length() - 1) == ':') {
                            words[i] = words[i].replace(words[i].substring(words[i].length() - 1), "");
                            String parts[] = words[i].split("@");
                            words[i] = "@" + parts[1];
                        } else {
                            String parts[] = words[i].split("@");
                            words[i] = "@" + parts[1];
                        }

                        if (words[i].matches(".*\\p{Punct}")) {
                            words[i] = "@" + words[i].replaceAll("\\p{P}", "");
                        }

                        System.out.println(words[i]);

                        BasicDBObject document = new BasicDBObject();
                        document.put("user", user);
                        document.put("timestamp", new Date());
                        document.put("mentioned_user", words[i]);
                        coll2.insert(document);
                        bool3 = true;
                    }
                    if (words[i].matches("^(https?|ftp)://.*$")) { // find urls
                        String shortenedUrl = words[i];
                        String expandedURL = expandUrl(shortenedUrl);   // expand Urls      
                        System.out.println(expandedURL);
                        BasicDBObject document = new BasicDBObject();
                        document.put("user", user);
                        document.put("timestamp", new Date());
                        document.put("URL", expandedURL);
                        coll2.insert(document);
                        bool4 = true;
                    }
                } // if a user's tweet doesn't have one or many of the above entites save the name of the entity as NULL
                if (bool1 == false) {
                    BasicDBObject document = new BasicDBObject();
                    document.put("user", user);
                    document.put("timestamp", new Date());
                    document.put("retweeted_tweet", null);
                    coll2.insert(document);
                }
                if (bool2 == false) {
                    BasicDBObject document = new BasicDBObject();
                    document.put("user", user);
                    document.put("timestamp", new Date());
                    document.put("hashtag", null);
                    coll2.insert(document);
                }
                if (bool3 == false) {
                    BasicDBObject document = new BasicDBObject();
                    document.put("user", user);
                    document.put("timestamp", new Date());
                    document.put("mentioned_user", null);
                    coll2.insert(document);
                }
                if (bool4 == false) {
                    BasicDBObject document = new BasicDBObject();
                    document.put("user", user);
                    document.put("timestamp", new Date());
                    document.put("URL", null);
                    coll2.insert(document);
                }
                System.out.println();
            }

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        try {
            // To connect to mongodb server
            MongoClient mongoClient = new MongoClient("localhost", 27017);

            // Now connect to your databases
            DB db = mongoClient.getDB("tweetDB3");
            System.out.println("Connect to database successfully");

            DBCollection coll = db.getCollection("test2");

            DBCursor cursor = coll.find();
            ArrayList<String> arraylist1 = new ArrayList<>();
            ArrayList<String> arraylist2 = new ArrayList<>();
            ArrayList<String> arraylist3 = new ArrayList<>();
            String name1 = "", name3 = "", name5 = "";
            // Compute Simillarity matrixes and save them in cvs files
            while (cursor.hasNext()) {
                BasicDBObject curr = (BasicDBObject) cursor.next();

                if (curr.containsField("retweeted_tweet")) { // save all the retweeted_tweets of a user's tweet
                    if (curr.get("retweeted_tweet") != null) {
                        String name = curr.get("user").toString();
                        String tweet = curr.get("retweeted_tweet").toString();
                        hashmap2.put(name, tweet);
                    } else {
                        String name = curr.get("user").toString();
                        hashmap2.put(name, "");
                    }
                }

                if (curr.containsField("hashtag")) {  //save all the hashtags of a user's tweet
                    if (curr.get("hashtag") != null) {
                        String name2 = curr.get("user").toString();
                        String hashtag = curr.get("hashtag").toString();

                        if (!name1.equals(name2)) {
                            hashmap.put(name1, arraylist1);
                            arraylist1 = new ArrayList<>();
                            arraylist1.add(hashtag);
                            name1 = name2;
                        } else {
                            arraylist1.add(hashtag);
                        }
                    } else {
                        String name2 = curr.get("user").toString();
                        hashmap.put(name1, arraylist1);
                        arraylist1 = new ArrayList<>();
                        name1 = name2;
                    }

                }

                if (curr.containsField("URL")) { //save all the URLs of a user's tweet
                    if (curr.get("URL") != null) {
                        String name4 = curr.get("user").toString();
                        String url = curr.get("URL").toString();

                        if (!name3.equals(name4)) {
                            hashmap3.put(name3, arraylist2);
                            arraylist2 = new ArrayList<>();
                            arraylist2.add(url);
                            name3 = name4;
                        } else {
                            arraylist2.add(url);
                        }
                    } else {
                        String name4 = curr.get("user").toString();
                        hashmap3.put(name3, arraylist2);
                        arraylist2 = new ArrayList<>();
                        name3 = name4;
                    }

                }
                if (curr.containsField("mentioned_user")) { //save all the mentions of a user's tweet
                    if (curr.get("mentioned_user") != null) {
                        String name6 = curr.get("user").toString();
                        String mention = curr.get("mentioned_user").toString();

                        if (!name5.equals(name6)) {
                            hashmap4.put(name5, arraylist3);
                            arraylist3 = new ArrayList<>();
                            arraylist3.add(mention);
                            name5 = name6;
                        } else {
                            arraylist3.add(mention);
                        }
                    } else {
                        String name6 = curr.get("user").toString();
                        hashmap4.put(name5, arraylist3);
                        arraylist3 = new ArrayList<>();
                        name5 = name6;
                    }

                }

            }
            // compute the similatiry matrix of user's retweeted tweets using LevenshteinDistance
            ArrayList<String> keys = new ArrayList<>(hashmap2.keySet());
            int size = hashmap2.size();
            int k;
            Double[][] tweetSim = new Double[size][size];
            for (int i = 0; i < size - 1; i++) {
                for (int j = i; j < size - 1; j++) {
                    String tweets1 = (String) hashmap2.values().toArray()[i];
                    String tweets2 = (String) hashmap2.values().toArray()[j];
                    tweetSim[i + 1][j + 1] = tweetSim[j + 1][i + 1] = LevenshteinDistance(tweets1, tweets2);
                    // System.out.println(printDistance(tweets1,tweets2)+"\n"+tweets1+"\n"+tweets2+"\n");
                }
            }

            System.out.println("------------------------RETWEETED TWEETS---------------------");
            FileWriter writer = new FileWriter("retweeted_tweets.csv", true);
            // save similarity matrix in a cvs file
            writer.write(';');
            for (k = 0; k < size - 2; k++) {
                writer.append(keys.get(k));
                writer.write(';');
            }
            writer.append(keys.get(k));
            writer.append('\n');
            writer.flush();
            System.out.println();
            for (int i = 1; i < size; i++) {
                writer.append(keys.get(i - 1));
                writer.write(';');
                int j;
                for (j = 1; j < size - 1; j++) {
                    if (i == j) {
                        writer.append("1.0");
                    } else if (tweetSim[i][j] < 0.5) // reduce noise, deleting edges with similarity less than 0.2
                    {
                        writer.append("0.0");
                    } else {
                        writer.append(Double.toString(tweetSim[i][j]));
                    }
                    writer.write(';');
                    System.out.print(tweetSim[i][j] + " ");
                }
                if (i == j) {
                    writer.append("1.0");
                } else if (tweetSim[i][j] < 0.5) {
                    writer.append("0.0");
                } else {
                    writer.append(Double.toString(tweetSim[i][j]));
                }
                writer.append('\n');
                writer.flush();
                System.out.println();
            }
            writer.close();
            System.out.println();

            // compute the similatiry matrix of user's hashtags using Jaccard Similarity
            Double[][] hashtagSim = new Double[size][size];
            for (int i = 1; i < size; i++) {
                for (int j = i; j < size; j++) {
                    ArrayList<String> hashtags1 = (ArrayList<String>) hashmap.values().toArray()[i];
                    ArrayList<String> hashtags2 = (ArrayList<String>) hashmap.values().toArray()[j];

                    for (k = 0; k < hashtags1.size(); k++) {
                        hashtags1.set(k, hashtags1.get(k).toLowerCase());
                    }
                    for (k = 0; k < hashtags2.size(); k++) {
                        hashtags2.set(k, hashtags2.get(k).toLowerCase());
                    }
                    hashtagSim[i][j] = hashtagSim[j][i] = jaccardSim(hashtags1, hashtags2);
                }
            }
            System.out.println("------------------------HASHTAGS---------------------");
            FileWriter writer2 = new FileWriter("hashtags.csv", true);

            writer2.write(';');
            for (k = 0; k < size - 2; k++) {
                writer2.append(keys.get(k));
                writer2.write(';');
            }
            writer2.append(keys.get(k));
            writer2.append('\n');
            writer2.flush();
            System.out.println();
            for (int i = 1; i < size; i++) {
                writer2.append(keys.get(i - 1));
                writer2.write(';');
                int j;
                for (j = 1; j < size - 1; j++) {
                    if (i == j) {
                        writer2.append("1.0");
                    } else if (hashtagSim[i][j] < 0.0) {
                        writer2.append("0.0");
                    } else {
                        writer2.append(Double.toString(hashtagSim[i][j]));
                    }
                    writer2.write(';');
                    System.out.print(hashtagSim[i][j] + " ");
                }
                if (i == j) {
                    writer2.append("1.0");
                } else if (hashtagSim[i][j] < 0.0) {
                    writer2.append("0.0");
                } else {
                    writer2.append(Double.toString(hashtagSim[i][j]));
                }
                writer2.append('\n');
                writer2.flush();
                System.out.println();
            }
            writer2.close();

            Double[][] urlSim = new Double[size][size];
            for (int i = 1; i < size; i++) {
                for (int j = i; j < size; j++) {
                    ArrayList<String> urls1 = (ArrayList<String>) hashmap3.values().toArray()[i];
                    ArrayList<String> urls2 = (ArrayList<String>) hashmap3.values().toArray()[j];

                    urlSim[i][j] = urlSim[j][i] = jaccardSim(urls1, urls2);
                }
            }

            // compute the similatiry matrix of user's URLS using Jaccard Similarity
            System.out.println("------------------------URLS---------------------");
            FileWriter writer3 = new FileWriter("urls.csv", true);

            writer3.write(';');
            for (k = 0; k < size - 2; k++) {
                writer3.append(keys.get(k));
                writer3.write(';');
            }
            writer3.append(keys.get(k));
            writer3.append('\n');
            writer3.flush();
            System.out.println();
            for (int i = 1; i < size; i++) {
                writer3.append(keys.get(i - 1));
                writer3.write(';');
                int j;
                for (j = 1; j < size - 1; j++) {
                    if (i == j) {
                        writer3.append("1.0");
                    } else if (urlSim[i][j] < 0.0) {
                        writer3.append("0.0");
                    } else {
                        writer3.append(Double.toString(urlSim[i][j]));
                    }
                    writer3.write(';');
                    System.out.print(urlSim[i][j] + " ");
                }
                if (i == j) {
                    writer3.append("1.0");
                } else if (urlSim[i][j] < 0.0) {
                    writer3.append("0.0");
                } else {
                    writer3.append(Double.toString(urlSim[i][j]));
                }
                writer3.append('\n');
                writer3.flush();
                System.out.println();
            }
            writer3.close();

            Double[][] mentionSim = new Double[size][size];
            for (int i = 1; i < size; i++) {
                for (int j = i; j < size; j++) {
                    ArrayList<String> mention1 = (ArrayList<String>) hashmap4.values().toArray()[i];
                    ArrayList<String> mention2 = (ArrayList<String>) hashmap4.values().toArray()[j];

                    mentionSim[i][j] = mentionSim[j][i] = jaccardSim(mention1, mention2);
                }
            }

            // compute the similatiry matrix of user's mentions using Jaccard Similarity
            System.out.println("------------------------MENTIONS---------------------");
            FileWriter writer4 = new FileWriter("mentions.csv", true);

            writer4.write(';');
            for (k = 0; k < size - 2; k++) {
                writer4.append(keys.get(k));
                writer4.write(';');
            }
            writer4.append(keys.get(k));
            writer4.append('\n');
            writer4.flush();
            System.out.println();
            for (int i = 1; i < size; i++) {
                writer4.append(keys.get(i - 1));
                writer4.write(';');
                int j;
                for (j = 1; j < size - 1; j++) {
                    if (i == j) {
                        writer4.append("1.0");
                    } else if (mentionSim[i][j] < 0.0) {
                        writer4.append("0.0");
                    } else {
                        writer4.append(Double.toString(mentionSim[i][j]));
                    }
                    writer4.write(';');
                    System.out.print(mentionSim[i][j] + " ");
                }
                if (i == j) {
                    writer4.append("1.0");
                } else if (mentionSim[i][j] < 0.0) {
                    writer4.append("0.0");
                } else {
                    writer4.append(Double.toString(mentionSim[i][j]));
                }
                writer4.append('\n');
                writer4.flush();
                System.out.println();
            }
            writer4.close();

            // compute the final similatiry matrix getting the average of all the 4 similarity matrices
            Double[][] finalSim = new Double[size][size];
            for (int i = 1; i < size; i++) {
                for (int j = i; j < size; j++) {
                    if (mentionSim[i][j] == -1.0 && hashtagSim[i][j] == -1.0 && urlSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = 0.0;
                    } else if (mentionSim[i][j] == -1.0 && hashtagSim[i][j] == -1.0 && urlSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = tweetSim[i][j];
                    } else if (mentionSim[i][j] == -1.0 && hashtagSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = urlSim[i][j];
                    } else if (mentionSim[i][j] == -1.0 && urlSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = hashtagSim[i][j];
                    } else if (hashtagSim[i][j] == -1.0 && urlSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = mentionSim[i][j];
                    } else if (mentionSim[i][j] == -1.0 && hashtagSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((tweetSim[i][j] + urlSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (mentionSim[i][j] == -1.0 && urlSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((tweetSim[i][j] + hashtagSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (mentionSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((hashtagSim[i][j] + urlSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (hashtagSim[i][j] == -1.0 && urlSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((mentionSim[i][j] + tweetSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (hashtagSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((mentionSim[i][j] + urlSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (urlSim[i][j] == -1.0 && tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((mentionSim[i][j] + hashtagSim[i][j]) / (double) 2 * 100.0) / 100.0;
                    } else if (mentionSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((hashtagSim[i][j] + tweetSim[i][j] + urlSim[i][j]) / (double) 3 * 100.0) / 100.0;
                    } else if (hashtagSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((mentionSim[i][j] + tweetSim[i][j] + urlSim[i][j]) / (double) 3 * 100.0) / 100.0;
                    } else if (urlSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((hashtagSim[i][j] + tweetSim[i][j] + mentionSim[i][j]) / (double) 3 * 100.0) / 100.0;
                    } else if (tweetSim[i][j] == -1.0) {
                        finalSim[i][j] = finalSim[j][i] = Math.round((hashtagSim[i][j] + mentionSim[i][j] + urlSim[i][j]) / (double) 3 * 100.0) / 100.0;
                    } else {
                        finalSim[i][j] = finalSim[j][i] = Math.round((mentionSim[i][j] + hashtagSim[i][j] + urlSim[i][j] + tweetSim[i][j]) / (double) 4 * 100.0) / 100.0;
                    }
                    // reduce noise, deleting edges with similarity less than 0.2
                    if (finalSim[i][j] < 0.2) {
                        finalSim[i][j] = finalSim[j][i] = 0.0;
                    }
                }
            }
            System.out.println("------------------------FINAL---------------------");
            FileWriter writer5 = new FileWriter("final.csv", true);

            writer5.write(';');
            for (k = 0; k < size - 2; k++) {
                writer5.append(keys.get(k));
                writer5.write(';');
            }
            writer5.append(keys.get(k));
            writer5.append('\n');
            writer5.flush();
            System.out.println();
            for (int i = 1; i < size; i++) {
                writer5.append(keys.get(i - 1));
                writer5.write(';');
                int j;
                for (j = 1; j < size - 1; j++) {
                    if (i == j) {
                        writer5.append("1.0");
                    } else {
                        writer5.append(Double.toString(finalSim[i][j]));
                    }
                    writer5.write(';');
                    System.out.print(finalSim[i][j] + " ");
                }
                if (i == j) {
                    writer5.append("1.0");
                } else {
                    writer5.append(Double.toString(finalSim[i][j]));
                }
                writer5.append('\n');
                writer5.flush();
                System.out.println();
            }
            writer5.close();

        } catch (Exception e) {

            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public static String expandUrl(String shortenedUrl) throws IOException {
        URL url = new URL(shortenedUrl);
        // open connection
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

        // stop following browser redirect
        httpURLConnection.setInstanceFollowRedirects(false);

        // extract location header containing the actual destination URL
        String expandedURL = httpURLConnection.getHeaderField("Location");
        httpURLConnection.disconnect();

        return expandedURL;
    }

    public static double jaccardSim(ArrayList<String> array1, ArrayList<String> array2) {
        ArrayList<String> list = new ArrayList<>();
        Set<String> set = new HashSet<>();

        for (String t : array1) {
            if (array2.contains(t)) {
                list.add(t);
            }
        }
        set.addAll(array1);
        set.addAll(array2);

        if (set.isEmpty()) {
            /* if both  arrays are empty set similarity as -1.0 so in the final similarity matrix we don't have to 
            include this entity, to the average of similarity*/
            return -1.00;
        } else {
            return Math.round((list.size() / (double) set.size()) * 100.0) / 100.0;
        }
    }

    public static double LevenshteinDistance(String s1, String s2) {
        double similarityOfStrings = 0;
        int editDistance = 0;
        if (s1.length() < s2.length()) { // s1 should always be bigger
            String swap = s1;
            s1 = s2;
            s2 = swap;
        }
        int bigLen = s1.length();
        editDistance = computeEditDistance(s1, s2);
        if (bigLen == 0) {
            similarityOfStrings = 1; // both strings are zero length 
        } else {
            similarityOfStrings = (bigLen - editDistance) / (double) bigLen;
        }
        if (s1.isEmpty() && s2.isEmpty()) {
            /* if both  arrays are empty set similarity as -1.0 so in the final similarity matrix we don't have to 
            include this entity, to the average of similarity*/
            return -1.0;
        } else {
            return Math.round(similarityOfStrings * 100.0) / 100.0;
        }
    }

    static int computeEditDistance(String s1, String s2) {
        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue),
                                    costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}
