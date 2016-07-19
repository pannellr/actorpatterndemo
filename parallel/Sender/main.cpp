#include <boost/mpi/environment.hpp>
#include <boost/mpi/communicator.hpp>
#include <iostream>

namespace mpi = boost::mpi;

void helloMPI() {
    mpi::environment env;
    mpi::communicator world;
    std::cout << "I am process " << world.rank() << " of " << world.size()
              << "." << std::endl;
}

void pointToPointCommunication() {
    mpi::environment env;
    mpi::communicator world;

    if (world.rank() != 0) {
        world.send(1, 0, std::string("Hello"));
        std::string msg;
        world.recv(1, 1, msg);
        std::cout << msg << "!" << std::endl;
    }
}

int main(int argc, char* argv[]) {
    pointToPointCommunication();
    return 0;
}
