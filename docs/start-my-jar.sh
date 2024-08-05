
# ps -ef |grep my-common-util.jar | grep -v grep | awk '{print $2}' | xargs -I{} kill -9 {}
cnt=`ps -ef |grep my-common-util.jar | grep -v grep | awk '{print $2}'`
if [ -z "$cnt" ]; then 
    cd /root/my-common-util 
    echo "start-xxx:`date`" > A-out.log
    source /etc/profile
    nohup java -Xms512m -Xmx1024m -Xss256k -jar /root/my-common-util/my-common-util.jar --spring.profiles.active=local >> A-out.log 2>&1 &
    echo "start-xxx:`date`" >> A-out.log
fi

# cd ~ && nohup java -Xms512m -Xmx1024m -Xss256k -jar my-common-util.jar --spring.profiles.active=local > A-out.log 2>&1 &

