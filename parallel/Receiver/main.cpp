#include <boost/mpi/environment.hpp>
#include <boost/mpi/communicator.hpp>
#include <iostream>

namespace mpi = boost::mpi;

void pointToPointCommunication() {
    mpi::environment env;
    mpi::communicator world;

    if (world.rank() == 0) {
        std::string msg;
        world.recv(0, 0, msg);
        std::cout << msg << ", ";
        std::cout.flush();
        world.send(0, 1, std::string("world"));
    }

}

int main(int argc, char* argv[]) {
    pointToPointCommunication();
    return 0;
}
