/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package twitter;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import java.net.UnknownHostException;
import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.UserMentionEntity;
import twitter4j.conf.ConfigurationBuilder;

public class CollectData {

    private ConfigurationBuilder cb;
    private DB db;
    private DBCollection items;

    /**
     * static block used to construct a connection with tweeter with twitter4j
     * configuration with provided settings. This configuration builder will be
     * used for next search action to fetch the tweets from twitter.com.
     */
    public static void main(String[] args) throws InterruptedException {

        CollectData stream = new CollectData();

        stream.loadMenu();

    }

    public void loadMenu() throws InterruptedException {

        connectdb("test2");

        cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true);

        cb.setOAuthConsumerKey("4xmQeiiPeFnsswzLbnPZNshJg");
        cb.setOAuthConsumerSecret("OhRZt5L7vmNi89VT5uMWYogzfLbiIRBGJ1pcBo1PY6fCo8VULg");
        cb.setOAuthAccessToken("793442004575354880-R0G5rfCFkwNZ6zdaRI9KB9nS5DOAwBL");
        cb.setOAuthAccessTokenSecret("SEHOSD2hjyhKVHugYkq2L6jqe1O8pHOBXRNXHY3aFIznq");

        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {

            public void onException(Exception arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onScrubGeo(long arg0, long arg1) {
                // TODO Auto-generated method stub

            }

            public void onStatus(Status status) {
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());

                BasicDBObject basicObj = new BasicDBObject();
                basicObj.put("user_name", status.getUser().getScreenName());
                basicObj.put("retweet_count", status.getRetweetCount());
                basicObj.put("coordinates", status.getGeoLocation());

                UserMentionEntity[] mentioned = status.getUserMentionEntities();
                basicObj.put("tweet_mentioned_count", mentioned.length);
                basicObj.put("tweet_ID", status.getId());
                basicObj.put("tweet_text", status.getText());

                try {
                    items.insert(basicObj);
                } catch (Exception e) {
                    System.out.println("MongoDB Connection Error : " + e.getMessage());

                }

            }

            @Override
            public void onTrackLimitationNotice(int arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStallWarning(StallWarning sw) {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

        };
        FilterQuery fq = new FilterQuery();

        String keywords[] = {"NBAVote"};

        fq.track(keywords);

        twitterStream.addListener(listener);
        twitterStream.filter(fq);

    }

    public void connectdb(String keyword) {
        try {
            // on constructor load initialize MongoDB and load collection
            initMongoDB();
            items = db.getCollection(keyword);

            //make the tweet_ID unique in the database
            BasicDBObject index = new BasicDBObject("tweet_ID", 1);
            items.ensureIndex(index, new BasicDBObject("unique", true));

        } catch (MongoException ex) {
            System.out.println("MongoException :" + ex.getMessage());
        }

    }

    /**
     * initMongoDB been called in constructor so every object creation this
     * initialize MongoDB.
     */
    public void initMongoDB() throws MongoException {
        try {
            System.out.println("Connecting to Mongo DB..");
            Mongo mongo;
            mongo = new Mongo("127.0.0.1");
            db = mongo.getDB("tweetDB2");
        } catch (UnknownHostException ex) {
            System.out.println("MongoDB Connection Error :" + ex.getMessage());
        }
    }

}
