if [ -f "env.sh" ]
then
    source env.sh
else
    echo "env.sh not found, please copy env.sh.base and fill the respective values"
fi

TEST_DIR=$1
cd tests/"${TEST_DIR}" || exit
sh dotest.sh