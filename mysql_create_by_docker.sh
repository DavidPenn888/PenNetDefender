docker run -d --name mysql8_pennetdef --restart unless-stopped -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=pennetdef -e MYSQL_USER=user -e MYSQL_PASSWORD=123456 -p 3306:3306 mysql:8

echo "可能需要赋权允许root外部登录"