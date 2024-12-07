package com.dulich.toudulich.Service;

import com.dulich.toudulich.DTO.BookingDTO;
import com.dulich.toudulich.Model.BookingModel;
import com.dulich.toudulich.Model.TourModel;
import com.dulich.toudulich.Model.UserModel;
import com.dulich.toudulich.Repositories.BookingRepository;
import com.dulich.toudulich.Repositories.TourRepository;
import com.dulich.toudulich.Repositories.UserRepository;
import com.dulich.toudulich.exceptions.DataNotFoundException;
import com.dulich.toudulich.responses.BookingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookingService implements iBookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TourRepository tourRepository;

    @Override
    public BookingModel createBooking(BookingDTO bookingDTO) throws DataNotFoundException {
        UserModel existingUser = userRepository.findById(bookingDTO.getUserId())
                .orElseThrow(() ->
                        new DataNotFoundException("Can't find user with id: " + bookingDTO.getUserId()));

        TourModel existingTour = tourRepository.findById(bookingDTO.getTourId())
                .orElseThrow(() ->
                        new DataNotFoundException("Can't find tour with id: " + bookingDTO.getTourId()));

        BookingModel newBooking = BookingModel.builder()
                .userId(existingUser)
                .fullName(bookingDTO.getFullName())
                .phoneNumber(bookingDTO.getPhoneNumber())
                .tourId(existingTour)
                .tourName(bookingDTO.getTourName())
                .amount(bookingDTO.getAmount())
                .startDate(bookingDTO.getStartDate())
                .totalPrice(bookingDTO.getTotalPrice())
                .status(bookingDTO.getStatus())
                .notes(bookingDTO.getNotes())
                .build();
        return bookingRepository.save(newBooking);
    }

    @Override
    public Page<BookingResponse> getAllBooking(PageRequest pageRequest) {
        return bookingRepository.findAll(pageRequest).map(BookingResponse::fromBooking);
    }

    @Override
    public BookingModel getBookingById(int id) throws Exception {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Can't find booking with id: " + id));
    }

    @Override
    public BookingModel updateBooking(int bookingId, String status) throws Exception {
        BookingModel existingBooking = getBookingById(bookingId);
        if (existingBooking != null) {
            // Cập nhật chỉ trường status
            if (status != null) {
                existingBooking.setStatus(status);
            }
            return bookingRepository.save(existingBooking);
        }
        return null;
    }


    @Override
    public boolean deleteBooking(int bookingId) {
        Optional<BookingModel> optionalBooking = bookingRepository.findById(bookingId);
        if (optionalBooking.isPresent()) {
            bookingRepository.deleteById(bookingId);
            return true;
        } else {
            return false;
        }
    }

}
