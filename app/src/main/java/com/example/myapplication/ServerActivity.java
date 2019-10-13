package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.ContentValues;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerActivity extends AppCompatActivity {

    private final static String TAG = "ServerActivity";
    String create_table = "CREATE TABLE if not EXISTS table01" +
            "(_id INTEGER PRIMARY KEY autoincrement , name TEXT)";

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1) {
                String tmp = msg.obj.toString();
                client_name.setText(tmp);
                ContentValues values = new ContentValues();
                values.put("name", tmp);
                db.insert("table01", null, values);

            }
        }
    };
    ArrayList<Integer> myarray = new ArrayList<Integer>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = openOrCreateDatabase("mydb.db", MODE_PRIVATE, null);
        db.execSQL(create_table);
        db.execSQL("INSERT INTO table01 (name) values ('aaa')");
        initView();
        new ServerThread().start();
        new robotThread().start();
        for(int i=0;i<20;i++ ){
            myarray.add(i,0);
        }
    }

    TextView client_name;
    ListView list;
    Button button;
    Button delete;
    private DrawerLayout drawerLayout;

    public void initView() {
        client_name = (TextView) findViewById(R.id.client_name);
        list = (ListView) findViewById(R.id.list);
        button = (Button) findViewById(R.id.button);
        delete = (Button) findViewById(R.id.button2);
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {

                cursor = getCursor();
                UpdataAdapter(cursor,-1);
            }
        });

        delete.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.execSQL("delete from table01");
                db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'table01'");
                db.execSQL("UPDATE sqlite_sequence SET SEQ=0  WHERE name = 'table01'");
            }
        });
        list.setItemChecked(0, true);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(myarray.get(position)!=1) {
                    dialog(position);
                }
                else if (myarray.get(position)==1){
                    dialog_2(position);
                }
            }
        });

    }

    InputStream is = null;
    OutputStream os = null;
    InputStream is_r = null;
    OutputStream os_r = null;

    public void dialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final int pos = position;
        builder.setMessage("確定要出餐嗎?");
        builder.setCancelable(false);

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                send_robot("go");
                //db.execSQL("delete from table01 WHERE _id=s");
                //db.execSQL("UPDATE sqlite_sequence SET _id = 0 WHERE name = 'table01'");
                //db.execSQL(String.format("DELETE FROM %s WHERE %s = %d", "table01","_id",(pos+1);
                //db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'table01'");
                UpdataAdapter(cursor,pos);

                dialog.dismiss();

            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();


    }

    public void dialog_2 (int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final int pos = position;
        builder.setMessage("確定要收餐嗎?");
        builder.setCancelable(false);

        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                send_robot("receive");
                db.execSQL(String.format("DELETE FROM %s WHERE %s = %d", "table01","_id",pos+1));
                dialog.dismiss();
                cursor = getCursor();
                db.execSQL("UPDATE table01 SET _id=_id-1  WHERE _id > "+pos);
                UpdataAdapter(cursor,pos);
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();



    }

    public class ServerThread extends Thread {

        final int socketServerPORT = 8080;
        ServerSocket serverSocket;
        Socket socket;


        public ServerThread() {
        }

        @Override
        public void run() {
            super.run();
            try {
                serverSocket = new ServerSocket(socketServerPORT);
                socket = serverSocket.accept();

                is = socket.getInputStream();
                os = socket.getOutputStream();

                new ReadThread(socket).start();
                send("server connect success");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class robotThread extends Thread {

        final int robotPORT = 8888;
        ServerSocket robotSocket;
        Socket socket_robot;

        public robotThread() {
        }

        @Override
        public void run() {
            super.run();
            try {

                robotSocket = new ServerSocket(robotPORT);
                socket_robot = robotSocket.accept();

                is_r = socket_robot.getInputStream();
                os_r = socket_robot.getOutputStream();
                send_robot("server connect success");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ReadThread extends Thread {

        public Socket socket;

        public ReadThread(Socket socket) {
            this.socket = socket;
        }


        @Override
        public void run() {
            while (socket.isConnected()) {
                byte[] buffer = new byte[128];
                int count = 0;
                if (socket != null) {
                    try {
                        String tmp;
                        count = is.read(buffer);
                        tmp = new String(buffer, 0, count, "utf-8");
                        Message message = new Message();
                        message.arg1 = 1;
                        message.obj = tmp;
                        handler.sendMessage(message);

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "ReadThread IOE");
                        break;
                    }
                }
            }
        }
    }

    public void send(final String tmp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (os != null) {
                    try {
                        os.write(tmp.getBytes("UTF-8"));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "send IOE");
                    }
                }
            }
        }).start();
    }

    public void send_robot(final String tmp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (os_r != null) {
                    try {
                        os_r.write(tmp.getBytes("UTF-8"));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG, "send IOE");
                    }
                }
            }
        }).start();
    }

    SQLiteDatabase db;
    Cursor cursor;

    public Cursor getCursor() {
        Cursor cursor = db.rawQuery("SELECT * FROM table01 ", null);
        return cursor;
    }

    public void UpdataAdapter(Cursor cursor , int position) {
        if (cursor != null && cursor.getCount() >= 0) {
            MyCursorAdapter adapter = new MyCursorAdapter(this,
                    android.R.layout.simple_expandable_list_item_2,
                    cursor,
                    new String[]{"_id", "name"},
                    new int[]{android.R.id.text1, android.R.id.text2},0) {

            };
            adapter.setSelectedPosition(position);
            if(adapter.selectedPosition!=-1 && myarray.get(adapter.selectedPosition)!=1) {
                myarray.set(adapter.selectedPosition,1);
            }
            else if(adapter.selectedPosition!=-1 && myarray.get(adapter.selectedPosition)==1){
                myarray.set(adapter.selectedPosition,0);
            }
            //db.execSQL("UPDATE sqlite_sequence SET SEQ=0  WHERE name = 'table01'");
            list.setAdapter(adapter);
        }
    }


    private class MyCursorAdapter extends SimpleCursorAdapter {
        private int selectedPosition = -1;

        public void setSelectedPosition(int position) {
            selectedPosition = position;
        }

        public MyCursorAdapter(Context context, int layout, Cursor c,
                               String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

                View view = super.getView(position, convertView, parent);
                TextView tvContent = (TextView) view.findViewById(R.id.text1);
                client_name.setText(String.valueOf(myarray.get(position)));

                if(myarray.get(position) == 1) {
                    view.setBackgroundColor(Color.RED);
                }
                else if (myarray.get(position)==0){
                    view.setBackgroundColor(Color.argb(255, 224, 243, 250));
                }

                return view;
        }
    }
}