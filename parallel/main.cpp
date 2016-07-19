#include <boost/mpi/environment.hpp>
#include <boost/mpi/communicator.hpp>
#include <iostream>

namespace mpi = boost::mpi;

void helloMPI() {
    mpi::communicator world;
    std::cout << "I am process " << world.rank() << " of " << world.size()
              << "." << std::endl;
}

void synchronousPointToPontCommunication() {
    mpi::communicator world;

    // The zeroeth process will send Hello
    if (world.rank() == 0) {
        world.send(1, 0, std::string("Hello"));
        std::string msg;
        world.recv(1, 1, msg);
        std::cout << msg << "!" << std::endl;
    } else { // Another process will send world
        std::string msg;
        world.recv(0, 0, msg);
        std::cout << msg << ", ";
        std::cout.flush();
        world.send(0, 1, std::string("world"));
    }
}

int main(int argc, char* argv[]) {
    mpi::environment env(argc, argv);
    helloMPI();
    return 0;
}
